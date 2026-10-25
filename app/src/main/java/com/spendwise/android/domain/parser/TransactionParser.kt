package com.spendwise.android.domain.parser

import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.notification.model.RawNotificationPayload
import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

/**
 * Pure Kotlin deterministic Transaction Parser.
 *
 * Requirements & Rules:
 * 1. 100% on-device local rule-based parsing. No external LLMs or servers.
 * 2. Parses Indian currency formats (₹, INR, Rs., Rs) into integer paise (Long).
 * 3. Never confuses account balance mentions with transaction amounts.
 * 4. Determines debit vs credit direction only from explicit evidence.
 * 5. Identifies refunds, reversals, and failed transactions cautiously.
 * 6. Never treats a failed transaction as a completed expense.
 * 7. Never treats refunds/cashback as salary.
 * 8. Rejects OTPs, promotional loan offers, and balance-only inquiries.
 * 9. Assigns confidence scores and routes ambiguous cases to Review.
 */
class TransactionParser {

    fun parse(payload: RawNotificationPayload): ParserResult {
        // 1. Combine title, text, and bigText safely in memory
        val titleText = payload.title?.trim() ?: ""
        val bodyText = payload.text?.trim() ?: ""
        val bigBodyText = payload.bigText?.trim() ?: ""

        val rawFullText = "$titleText $bodyText $bigBodyText".trim()
        if (rawFullText.isBlank()) {
            return ParserResult.NotTransaction(NotTransactionReason.EMPTY_OR_NO_TEXT)
        }

        // Normalize whitespace and non-breaking spaces
        val normalizedText = normalizeWhitespace(rawFullText)
        val lowerText = normalizedText.lowercase(Locale.ROOT)

        // 2. Early rejection: OTP / 2FA / Authentication codes
        if (isOtpOrAuthNotification(lowerText)) {
            return ParserResult.NotTransaction(NotTransactionReason.AUTH_OR_OTP)
        }

        // 3. Early rejection: Promotional offers / loan solicitations / advertisements
        if (isPromotionalOrOffer(lowerText)) {
            return ParserResult.NotTransaction(NotTransactionReason.OFFER_OR_PROMOTION)
        }

        // 4. Failed / Declined transaction detection
        val isFailed = isFailedTransaction(lowerText)

        // 5. Direction Detection
        val direction = detectDirection(lowerText)

        // 6. Balance-only inquiry check (No debit/credit action, only balance report)
        if (direction == null && isBalanceInquiryOnly(lowerText)) {
            return ParserResult.NotTransaction(NotTransactionReason.BALANCE_INQUIRY_ONLY)
        }

        // 7. Amount vs. Balance Extraction
        val amountResult = extractTransactionAmountPaise(normalizedText)
        if (amountResult is AmountExtractionResult.NoAmountFound) {
            return ParserResult.NotTransaction(NotTransactionReason.UNSUPPORTED_LANGUAGE_OR_FORMAT)
        }

        val amountPaise: Long?
        var ambiguousAmounts = false
        when (amountResult) {
            is AmountExtractionResult.Success -> {
                amountPaise = amountResult.amountPaise
            }
            is AmountExtractionResult.AmbiguousMultipleAmounts -> {
                amountPaise = amountResult.firstAmountPaise
                ambiguousAmounts = true
            }
            is AmountExtractionResult.BalanceOnlyFound -> {
                return ParserResult.NotTransaction(NotTransactionReason.BALANCE_INQUIRY_ONLY)
            }
            else -> {
                amountPaise = null
            }
        }

        // 8. Refund / Reversal detection
        val isRefundOrReversal = isRefundOrReversal(lowerText)

        // 9. Merchant / Payee extraction & normalization
        val extractedMerchant = extractMerchant(normalizedText, lowerText)

        // 10. Category classification
        val category = classifyCategory(lowerText, extractedMerchant, isRefundOrReversal)

        // 11. Deduplication fingerprint with reference number support
        val refNumber = extractReferenceNumber(normalizedText)
        val fingerprint = computeFingerprint(
            payload.packageName,
            amountPaise ?: 0L,
            direction?.name ?: "UNKNOWN",
            extractedMerchant,
            payload.postTimeEpochMs,
            refNumber
        )

        // 12. Evaluate Confidence & Review Reasons
        val reviewReasons = mutableListOf<ReviewReason>()

        if (isFailed) {
            reviewReasons.add(ReviewReason.FAILED_TRANSACTION_ALERT)
        }
        if (ambiguousAmounts) {
            reviewReasons.add(ReviewReason.AMBIGUOUS_MULTIPLE_AMOUNTS)
        }
        if (isRefundOrReversal) {
            reviewReasons.add(ReviewReason.REFUND_OR_REVERSAL)
        }
        if (direction == null) {
            reviewReasons.add(ReviewReason.LOW_CONFIDENCE)
        }
        if (extractedMerchant == null || extractedMerchant.length <= 2) {
            reviewReasons.add(ReviewReason.UNCERTAIN_MERCHANT)
        }
        if (category == TransactionCategory.TRANSFER) {
            reviewReasons.add(ReviewReason.SUSPECTED_TRANSFER)
        }

        // Compute numerical confidence score [0.0 .. 1.0]
        var confidence = 0.95f
        if (isFailed) {
            confidence = 0.20f // Never confirm failed transactions automatically
        } else {
            if (ambiguousAmounts) confidence -= 0.25f
            if (isRefundOrReversal) confidence -= 0.15f
            if (direction == null) confidence -= 0.35f
            if (extractedMerchant == null) confidence -= 0.15f
            if (amountPaise == null || amountPaise <= 0L) confidence -= 0.50f
        }
        confidence = confidence.coerceIn(0.0f, 1.0f)

        val candidate = TransactionCandidate(
            amountPaise = amountPaise,
            currencyCode = "INR",
            direction = direction,
            merchant = extractedMerchant ?: "Unknown Payee",
            timestampEpochMs = payload.postTimeEpochMs,
            sourcePackage = payload.packageName,
            confidence = confidence,
            reviewReasons = reviewReasons,
            suggestedCategory = category,
            isRefundOrReversal = isRefundOrReversal,
            isFailed = isFailed,
            dedupeFingerprint = fingerprint
        )

        // Final decision: Parsed vs NeedsReview vs NotTransaction
        if (isFailed) {
            return ParserResult.NeedsReview(candidate, ReviewReason.FAILED_TRANSACTION_ALERT)
        }

        if (confidence >= 0.90f && reviewReasons.isEmpty() && amountPaise != null && direction != null) {
            return ParserResult.Parsed(candidate)
        }

        val primaryReason = reviewReasons.firstOrNull() ?: ReviewReason.LOW_CONFIDENCE
        return ParserResult.NeedsReview(candidate, primaryReason)
    }

    private fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("[\\u00A0\\u200B\\u200C\\u200D\\uFEFF\\s]+"), " ").trim()
    }

    private fun isOtpOrAuthNotification(lowerText: String): Boolean {
        val otpKeywords = listOf(
            "otp", "one time password", "verification code", "secret code",
            "do not share", "never share", "use code", "valid for", "is your login code"
        )
        // Must contain an auth keyword and not be an already debited/credited confirmation
        return otpKeywords.any { lowerText.contains(it) } && !lowerText.contains("successfully paid")
    }

    private fun isPromotionalOrOffer(lowerText: String): Boolean {
        val promoKeywords = listOf(
            "pre-approved", "pre approved", "instant loan", "get up to",
            "flat discount", "cashback offer", "apply now", "limited period offer",
            "claim your offer", "hurry", "win up to", "congratulations! you are eligible"
        )
        return promoKeywords.any { lowerText.contains(it) } &&
                !lowerText.contains("debited") &&
                !lowerText.contains("credited") &&
                !lowerText.contains("paid to")
    }

    private fun isFailedTransaction(lowerText: String): Boolean {
        val failedKeywords = listOf(
            "failed", "declined", "unsuccessful", "transaction failed",
            "payment failed", "could not be processed", "rejected", "payment was declined"
        )
        return failedKeywords.any { lowerText.contains(it) }
    }

    private fun isRefundOrReversal(lowerText: String): Boolean {
        val refundKeywords = listOf(
            "refund", "refunded", "reversal", "reversed", "money reversed", "money refunded"
        )
        return refundKeywords.any { lowerText.contains(it) }
    }

    private fun isBalanceInquiryOnly(lowerText: String): Boolean {
        val balanceOnlyKeywords = listOf(
            "your balance is", "account balance is", "clear balance is", "balance inquiry"
        )
        return balanceOnlyKeywords.any { lowerText.contains(it) }
    }

    private fun detectDirection(lowerText: String): TransactionDirection? {
        val debitKeywords = listOf(
            "debited", "spent", "paid to", "sent to", "sent ", "withdrawn", "payment of",
            "purchase of", "purchased at", "charged", "paid rs", "paid inr", "paid ₹",
            "transfer to", "transferred to"
        )
        val creditKeywords = listOf(
            "credited", "received from", "received rs", "received inr", "received ₹",
            "deposited", "added to your", "cashback received", "cashback credited",
            "refund credited", "refund of", "salary credited", "payroll"
        )

        val hasDebit = debitKeywords.any { lowerText.contains(it) }
        val hasCredit = creditKeywords.any { lowerText.contains(it) }

        return when {
            hasDebit && !hasCredit -> TransactionDirection.DEBIT
            hasCredit && !hasDebit -> TransactionDirection.CREDIT
            hasDebit && hasCredit -> {
                // If both present (e.g. "debited ... refund"), check which comes first or if refund
                if (lowerText.contains("refund") || lowerText.contains("reversal")) {
                    TransactionDirection.CREDIT
                } else {
                    TransactionDirection.DEBIT
                }
            }
            else -> null
        }
    }

    /**
     * Extracts transaction amounts while strictly filtering out account balances.
     */
    private fun extractTransactionAmountPaise(text: String): AmountExtractionResult {
        // Regex to capture amounts like:
        // ₹ 1,234.50, INR 500, Rs. 1500, Rs 50.00
        val pattern = Pattern.compile(
            "(?:₹|INR|Rs\\.?|RS\\.?)\\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)|([0-9]{1,3}(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:INR|Rs\\.?|rupees?)",
            Pattern.CASE_INSENSITIVE
        )

        val matcher = pattern.matcher(text)
        val foundAmounts = mutableListOf<ExtractedAmount>()

        while (matcher.find()) {
            val amountString = matcher.group(1) ?: matcher.group(2) ?: continue
            val startIndex = matcher.start()

            // Look back up to 35 characters before the match to detect balance prefixes
            val lookbackStart = (startIndex - 35).coerceAtLeast(0)
            val lookbackSubstring = text.substring(lookbackStart, startIndex).lowercase(Locale.ROOT)

            val isBalance = isPrecededByBalanceKeyword(lookbackSubstring)
            val paise = parseAmountStringToPaise(amountString)
            if (paise != null && paise > 0) {
                foundAmounts.add(ExtractedAmount(paise, isBalance))
            }
        }

        if (foundAmounts.isEmpty()) {
            return AmountExtractionResult.NoAmountFound
        }

        val nonBalanceAmounts = foundAmounts.filter { !it.isBalance }
        val balanceAmounts = foundAmounts.filter { it.isBalance }

        return when {
            nonBalanceAmounts.size == 1 -> {
                AmountExtractionResult.Success(nonBalanceAmounts.first().paise)
            }
            nonBalanceAmounts.size > 1 -> {
                // Multiple transaction amounts detected -> Ambiguous!
                AmountExtractionResult.AmbiguousMultipleAmounts(
                    firstAmountPaise = nonBalanceAmounts.first().paise,
                    allAmounts = nonBalanceAmounts.map { it.paise }
                )
            }
            balanceAmounts.isNotEmpty() -> {
                // Only balance amounts detected
                AmountExtractionResult.BalanceOnlyFound
            }
            else -> AmountExtractionResult.NoAmountFound
        }
    }

    private fun isPrecededByBalanceKeyword(lookback: String): Boolean {
        val balancePrefixes = listOf(
            "avl bal", "available bal", "avl. bal", "available balance",
            "updated bal", "clear bal", "total bal", "rem bal", "remaining bal",
            "bal:", "bal ", "balance:", "balance ", "a/c bal", "acct bal"
        )
        return balancePrefixes.any { lookback.contains(it) }
    }

    private fun parseAmountStringToPaise(rawAmount: String): Long? {
        return try {
            val clean = rawAmount.replace(",", "").trim()
            val parts = clean.split(".")
            val rupees = parts[0].toLong()
            val paise = if (parts.size > 1) {
                val fraction = parts[1].padEnd(2, '0').take(2)
                fraction.toLong()
            } else {
                0L
            }
            (rupees * 100L) + paise
        } catch (_: Exception) {
            null
        }
    }

    private fun extractMerchant(text: String, lowerText: String): String? {
        val merchantPatterns = listOf(
            Pattern.compile("(?i)(?:paid to|sent to|transfer to|transferred to)\\s+([A-Za-z0-9\\s&.'-]{2,40}?)(?:\\s+using|\\s+on|\\s+ref|\\s+upi|\\s+via|\\s+from|\\s+avl|\\s+bal|\\.|,|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(?:at)\\s+([A-Za-z0-9\\s&.'-]{2,35}?)(?:\\s+using|\\s+on|\\s+ref|\\s+upi|\\s+via|\\s+from|\\s+avl|\\s+bal|\\.|,|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(?:received\\s+(?:(?:₹|INR|Rs\\.?)\\s*[0-9,.]+\\s+)?from|received from|from)\\s+([A-Za-z0-9\\s&.'-]{2,35}?)(?:\\s+using|\\s+on|\\s+ref|\\s+upi|\\s+via|\\s+in|\\.|,|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(?:by)\\s+([A-Za-z0-9\\s&.'-]{2,35}?)(?:\\s+using|\\s+on|\\s+ref|\\s+upi|\\s+via|\\.|,|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(?:vpa\\s+)([a-zA-Z0-9.\\-_@]{3,40})", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && isValidMerchantCandidate(candidate)) {
                    return cleanMerchantName(candidate)
                }
            }
        }

        // Check for known brand names in the text
        val knownMerchants = listOf(
            "Swiggy", "Zomato", "Starbucks", "Uber", "Ola", "Rapido",
            "Amazon", "Flipkart", "Myntra", "Blinkit", "Zepto", "Instamart",
            "Airtel", "Jio", "Bescom", "Netflix", "Spotify", "BookMyShow",
            "Apollo", "Pharmeasy", "DMart", "Reliance", "Tata Neu"
        )
        for (brand in knownMerchants) {
            if (lowerText.contains(brand.lowercase(Locale.ROOT))) {
                return brand
            }
        }

        if (lowerText.contains("salary") || lowerText.contains("payroll")) {
            return "Salary"
        }

        return null
    }

    private fun isValidMerchantCandidate(name: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        val invalidTokens = listOf("your a/c", "acct", "account", "bank", "credit card", "debit card")
        return invalidTokens.none { lower.contains(it) } && name.length in 3..40
    }

    private fun cleanMerchantName(name: String): String {
        return name
            .replace(Regex("(?i)\\b(?:pvt|ltd|limited|llp|inc|co|corp|via|upi)\\b"), "")
            .replace(Regex("[^a-zA-Z0-9\\s&.-]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun classifyCategory(
        lowerText: String,
        merchant: String?,
        isRefund: Boolean
    ): TransactionCategory {
        if (isRefund) {
            return TransactionCategory.OTHER
        }

        if (lowerText.contains("salary") || lowerText.contains("payroll")) {
            return TransactionCategory.INCOME
        }

        if (lowerText.contains("self transfer") || lowerText.contains("transferred to own")) {
            return TransactionCategory.TRANSFER
        }

        val combined = "$lowerText ${merchant?.lowercase(Locale.ROOT) ?: ""}"

        val foodKeywords = listOf("swiggy", "zomato", "starbucks", "mcdonald", "kfc", "burger", "cafe", "restaurant", "pizza", "blinkit", "zepto", "instamart", "chai", "bakery", "grocery")
        if (foodKeywords.any { combined.contains(it) }) return TransactionCategory.FOOD

        val transportKeywords = listOf("uber", "ola", "rapido", "metro", "petrol", "diesel", "fuel", "indian oil", "bpcl", "hpcl", "shell", "irctc", "makemytrip", "redbus", "flight", "toll")
        if (transportKeywords.any { combined.contains(it) }) return TransactionCategory.TRANSPORT

        val shoppingKeywords = listOf("amazon", "flipkart", "myntra", "zara", "nykaa", "ajio", "retail", "supermarket", "dmart", "store", "mall")
        if (shoppingKeywords.any { combined.contains(it) }) return TransactionCategory.SHOPPING

        val billKeywords = listOf("electricity", "water", "gas", "bescom", "airtel", "jio", "vodafone", "vi", "broadband", "tata play", "recharge", "bill payment", "lic", "insurance")
        if (billKeywords.any { combined.contains(it) }) return TransactionCategory.BILLS

        val healthKeywords = listOf("apollo", "pharmeasy", "1mg", "hospital", "clinic", "pharmacy", "medplus", "doctor", "practo", "diagnostic")
        if (healthKeywords.any { combined.contains(it) }) return TransactionCategory.HEALTH

        val entertainmentKeywords = listOf("netflix", "spotify", "bookmyshow", "prime video", "hotstar", "disney", "youtube", "pvr", "inox", "cinema", "movie", "steam")
        if (entertainmentKeywords.any { combined.contains(it) }) return TransactionCategory.ENTERTAINMENT

        return TransactionCategory.OTHER
    }

    private fun extractReferenceNumber(text: String): String? {
        val patterns = listOf(
            Pattern.compile("(?i)(?:upi\\s+ref(?:\\s+no)?|ref\\s+no|rrn|txn\\s+id|ref)\\s*[:.]?\\s*([A-Za-z0-9]{8,22})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)(?:ref(?:\\s+is|:)?\\s*)([0-9]{8,18})", Pattern.CASE_INSENSITIVE)
        )
        for (p in patterns) {
            val m = p.matcher(text)
            if (m.find()) {
                val ref = m.group(1)?.trim()
                if (!ref.isNullOrBlank()) return ref
            }
        }
        return null
    }

    private fun computeFingerprint(
        sourcePackage: String,
        amountPaise: Long,
        direction: String,
        merchant: String?,
        postTimeEpochMs: Long,
        refNumber: String?
    ): String {
        if (!refNumber.isNullOrBlank()) {
            return "REF:$sourcePackage:$amountPaise:$direction:$refNumber"
        }

        val normalizedMerchant = merchant?.trim()?.lowercase(Locale.ROOT) ?: "none"
        val timeBucket = postTimeEpochMs / 300_000L
        val rawKey = "$sourcePackage|$amountPaise|$direction|$normalizedMerchant|$timeBucket"

        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(rawKey.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            rawKey
        }
    }

    private data class ExtractedAmount(val paise: Long, val isBalance: Boolean)

    private sealed class AmountExtractionResult {
        data class Success(val amountPaise: Long) : AmountExtractionResult()
        data class AmbiguousMultipleAmounts(val firstAmountPaise: Long, val allAmounts: List<Long>) : AmountExtractionResult()
        object BalanceOnlyFound : AmountExtractionResult()
        object NoAmountFound : AmountExtractionResult()
    }
}
