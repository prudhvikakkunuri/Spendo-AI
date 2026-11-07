package com.spendwise.android.domain.banking

import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.notification.model.RawNotificationPayload
import java.util.Locale
import java.util.regex.Pattern

/**
 * Common interface for bank-specific message parsing.
 */
interface BankMessageParser {
    val bankName: String
    fun canHandle(senderOrPackage: String, text: String): Boolean
    fun parse(payload: RawNotificationPayload): ParsedBankMessage?
}

/**
 * Base helper with robust Indian banking regex utilities.
 */
abstract class BaseIndianBankParser : BankMessageParser {

    protected fun extractAmountPaise(text: String): Long? {
        // Strict pattern matching transaction amount while ignoring balance prefixes
        val pattern = Pattern.compile(
            "(?:debited\\s+(?:for|by|with)?|spent|sent|paid|withdrawn|(?:cash\\s+)?withdrawal\\s+(?:of|at|from)?|credited\\s+(?:for|by|with)?|received|purchase\\s+of)\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1) ?: return null
            return parseToPaise(raw)
        }

        // Secondary fallback pattern for leading currency
        val fallbackPattern = Pattern.compile(
            "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|credited|withdrawn|spent|sent|paid)",
            Pattern.CASE_INSENSITIVE
        )
        val fallbackMatcher = fallbackPattern.matcher(text)
        if (fallbackMatcher.find()) {
            val raw = fallbackMatcher.group(1) ?: return null
            return parseToPaise(raw)
        }

        return null
    }

    protected fun extractAvailableBalancePaise(text: String): Long? {
        val pattern = Pattern.compile(
            "(?:avl(?:\\.|\\s+)?bal|available\\s+bal(?:ance)?|clear\\s+bal|total\\s+bal|bal:?)\\s*(?:is|:)?\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1) ?: return null
            return parseToPaise(raw)
        }
        return null
    }

    protected fun extractReferenceNumber(text: String): String? {
        val patterns = listOf(
            Pattern.compile("(?i)info\\s*:\\s*([A-Za-z0-9/_-]{6,35})"),
            Pattern.compile("(?i)(?:upi\\s*ref(?:\\s*no)?|rrn|ref\\s*no|txn\\s*id|utr)[:\\s/]+([A-Za-z0-9/_-]{6,35})"),
            Pattern.compile("(?i)UPI/([0-9]{10,25})"),
            Pattern.compile("(?i)Ref[:\\s]+([A-Za-z0-9/_-]{6,35})")
        )
        for (pat in patterns) {
            val m = pat.matcher(text)
            if (m.find()) {
                val ref = m.group(1)
                if (!ref.isNullOrBlank()) return ref.trim()
            }
        }
        return null
    }

    protected fun extractAccountSuffix(text: String): String? {
        val patterns = listOf(
            Pattern.compile("(?i)(?:a/?c|acc?t?|account|card)\\s*(?:no\\.?|number|num)?[:\\s]*(?:ending\\s*(?:in|with)?\\s*)?[0-9*xX.•\u2022-]*\\s*([0-9]{4})\\b"),
            Pattern.compile("(?i)(?:ending\\s*(?:in|with)?\\s*[*xX.•\u2022-]*|[*xX.•\u2022-]{2,})\\s*([0-9]{4})\\b"),
            Pattern.compile("(?i)(?:a/?c|acc?t?|account)[:\\s]+([0-9]{4})\\b")
        )
        for (pat in patterns) {
            val m = pat.matcher(text)
            if (m.find()) {
                val suffix = m.group(1)
                if (!suffix.isNullOrBlank()) {
                    return suffix
                }
            }
        }
        return null
    }

    protected fun extractDateEpochMs(text: String): Long? {
        val datePattern = Pattern.compile(
            "(?:on\\s+)?([0-3]?[0-9][-/](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|[0-1]?[0-9])[-/][0-9]{2,4})",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = datePattern.matcher(text)
        if (matcher.find()) {
            val dateStr = matcher.group(1) ?: return null
            val formats = listOf("dd-MMM-yyyy", "dd-MMM-yy", "dd/MM/yyyy", "dd-MM-yyyy")
            for (fmt in formats) {
                try {
                    val sdf = java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH)
                    sdf.isLenient = false
                    val date = sdf.parse(dateStr)
                    if (date != null) return date.time
                } catch (_: Exception) {}
            }
        }
        return null
    }

    protected fun isNonTransaction(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        // 1. OTP messages
        if (lower.contains("otp") || lower.contains("one time password") || lower.contains("verification code") || lower.contains("secret code")) {
            return true
        }
        // 2. Promotional messages
        if (lower.contains("pre-approved") || lower.contains("apply for loan") || lower.contains("apply now") || lower.contains("congratulations! you are eligible")) {
            return true
        }
        // 3. Failed transactions
        if (lower.contains("failed") || lower.contains("declined") || lower.contains("unsuccessful") || lower.contains("could not be processed")) {
            return true
        }
        return false
    }

    protected fun parseToPaise(raw: String): Long? {
        return try {
            val clean = raw.replace(",", "").trim()
            val parts = clean.split(".")
            val rupees = parts[0].toLong()
            val paise = if (parts.size > 1) {
                parts[1].padEnd(2, '0').take(2).toLong()
            } else {
                0L
            }
            (rupees * 100L) + paise
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * 1. Indian Bank Message Parser
 * Handles official SMS and notifications from Indian Bank.
 */
class IndianBankParser : BaseIndianBankParser() {
    override val bankName: String = "Indian Bank"

    override fun canHandle(senderOrPackage: String, text: String): Boolean {
        val upper = (senderOrPackage + " " + text).uppercase(Locale.ROOT)
        return upper.contains("INDBNK") || upper.contains("INDIAN BANK") || upper.contains("INDIANBANK")
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        if (isNonTransaction(combined)) return null

        val lower = combined.lowercase(Locale.ROOT)
        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined)
        val dateMs = extractDateEpochMs(combined) ?: payload.postTimeEpochMs

        val isCredit = lower.contains("credited") || lower.contains("deposited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("withdrawn") || lower.contains("spent")
        val isRefund = lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")
        val direction = if (isRefund || (isCredit && !isDebit)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")
        val isSalary = lower.contains("salary")

        val txType = when {
            isRefund -> BankTransactionType.REFUND_REVERSAL
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            isSalary -> BankTransactionType.SALARY_CREDIT
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val sanitized = combined.replace(Regex("(?i)(?:not you\\?|if not you|call\\s+\\d+|for any query|toll free).*"), "")
        val merchantPattern = if (direction == TransactionDirection.CREDIT) {
            Pattern.compile("(?i)(?:from|received from|by)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+upi|\\s+avl|\\s+ref|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile("(?i)(?:towards|to|at|paid to)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+upi|\\s+avl|\\s+ref|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        }
        val mMatcher = merchantPattern.matcher(sanitized)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "Indian Bank Payee" else "Indian Bank Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(80),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = dateMs,
            sourcePackage = payload.packageName
        )
    }
}

/**
 * 2. Kotak Mahindra Bank Message Parser
 */
class KotakBankParser : BaseIndianBankParser() {
    override val bankName: String = "Kotak Mahindra Bank"

    override fun canHandle(senderOrPackage: String, text: String): Boolean {
        val upper = (senderOrPackage + " " + text).uppercase(Locale.ROOT)
        return upper.contains("KOTAK") || upper.contains("KOTAKB") || upper.contains("KMBL")
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        if (isNonTransaction(combined)) return null

        val lower = combined.lowercase(Locale.ROOT)
        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined)
        val dateMs = extractDateEpochMs(combined) ?: payload.postTimeEpochMs

        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn") || lower.contains("paid")
        val isRefund = lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")
        val direction = if (isRefund || (isCredit && !isDebit)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")

        val txType = when {
            isRefund -> BankTransactionType.REFUND_REVERSAL
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("salary") -> BankTransactionType.SALARY_CREDIT
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val sanitized = combined.replace(Regex("(?i)(?:not you\\?|if not you|call\\s+\\d+|for any query|toll free).*"), "")
        val merchantPattern = Pattern.compile("(?i)(?:to|info:?)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+upi|\\s+avl|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        val mMatcher = merchantPattern.matcher(sanitized)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "Kotak Payee" else "Kotak Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(80),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = dateMs,
            sourcePackage = payload.packageName
        )
    }
}

/**
 * 3. Karur Vysya Bank (KVB) Message Parser
 */
class KvbBankParser : BaseIndianBankParser() {
    override val bankName: String = "Karur Vysya Bank"

    override fun canHandle(senderOrPackage: String, text: String): Boolean {
        val upper = (senderOrPackage + " " + text).uppercase(Locale.ROOT)
        return upper.contains("KVBANK") || upper.contains("KVB") || upper.contains("KARUR VYSYA") || upper.contains("18005721916-KVB")
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        if (isNonTransaction(combined)) return null

        val lower = combined.lowercase(Locale.ROOT)
        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined)
        val dateMs = extractDateEpochMs(combined) ?: payload.postTimeEpochMs

        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn") || lower.contains("paid")
        val isRefund = lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")
        val direction = if (isRefund || (isCredit && !isDebit)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")

        val txType = when {
            isRefund -> BankTransactionType.REFUND_REVERSAL
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val sanitized = combined.replace(Regex("(?i)(?:not you\\?|if not you|call\\s+\\d+|for any query|toll free).*"), "")
        val merchantPattern = Pattern.compile("(?i)(?:towards|to|at)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+ref|\\s+clear|\\s+bal|\\s+avl|\\.|$)", Pattern.CASE_INSENSITIVE)
        val mMatcher = merchantPattern.matcher(sanitized)
        val merchant = if (mMatcher.find()) {
            mMatcher.group(1)?.trim() ?: "KVB Payee"
        } else {
            "KVB Payee"
        }

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.trim().take(80),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = dateMs,
            sourcePackage = payload.packageName
        )
    }
}

/**
 * 4. Generic Indian Bank Parser
 * Fallback parser capable of handling HDFC, SBI, ICICI, Axis, PNB, or any bank notification/SMS.
 */
class GenericBankParser : BaseIndianBankParser() {
    override val bankName: String = "Bank"

    override fun canHandle(senderOrPackage: String, text: String): Boolean {
        val lowerPkg = senderOrPackage.lowercase(Locale.ROOT)
        if (lowerPkg.contains("paisa") || lowerPkg.contains("phonepe") ||
            lowerPkg.contains("paytm") || lowerPkg.contains("dreamplug") ||
            lowerPkg.contains("npci") || lowerPkg.contains("bhim")
        ) {
            return false
        }
        val lower = (senderOrPackage + " " + text).lowercase(Locale.ROOT)
        return (lower.contains("debited") || lower.contains("credited") || lower.contains("withdrawn") || lower.contains("spent") || lower.contains("sent") || lower.contains("received") || lower.contains("refund")) &&
                (lower.contains("rs.") || lower.contains("rs ") || lower.contains("inr") || lower.contains("₹"))
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        if (isNonTransaction(combined)) return null

        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined)
        val dateMs = extractDateEpochMs(combined) ?: payload.postTimeEpochMs

        val lower = combined.lowercase(Locale.ROOT)
        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn") || lower.contains("paid") || lower.contains("spent")
        val isRefund = lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")
        val direction = if (isRefund || (isCredit && !isDebit)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val inferredBank = inferBankName(payload.packageName, combined)

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")
        val txType = when {
            isRefund -> BankTransactionType.REFUND_REVERSAL
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            lower.contains("salary") -> BankTransactionType.SALARY_CREDIT
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val sanitized = combined.replace(Regex("(?i)(?:not you\\?|if not you|call\\s+\\d+|for any query|toll free).*"), "")
        val merchantPattern = Pattern.compile("(?i)(?:towards|to|at)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+ref|\\s+clear|\\s+bal|\\s+avl|\\.|$)", Pattern.CASE_INSENSITIVE)
        val mMatcher = merchantPattern.matcher(sanitized)
        val merchant = if (mMatcher.find()) {
            mMatcher.group(1)?.trim() ?: "$inferredBank Payee"
        } else {
            "$inferredBank Payee"
        }

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = inferredBank,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.trim().take(80),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.95f else 0.88f,
            timestampEpochMs = dateMs,
            sourcePackage = payload.packageName
        )
    }

    private fun inferBankName(packageName: String, text: String): String {
        val upper = (packageName + " " + text).uppercase(Locale.ROOT)
        return when {
            upper.contains("HDFC") -> "HDFC Bank"
            upper.contains("SBI") || upper.contains("STATE BANK") -> "State Bank of India"
            upper.contains("ICICI") -> "ICICI Bank"
            upper.contains("AXIS") -> "Axis Bank"
            upper.contains("PNB") || upper.contains("PUNJAB NATIONAL") -> "Punjab National Bank"
            upper.contains("CANARA") -> "Canara Bank"
            upper.contains("BOB") || upper.contains("BANK OF BARODA") -> "Bank of Baroda"
            upper.contains("UNION") -> "Union Bank of India"
            upper.contains("IDFC") -> "IDFC FIRST Bank"
            upper.contains("INDUSIND") -> "IndusInd Bank"
            upper.contains("YES") -> "Yes Bank"
            else -> "Bank"
        }
    }
}

/**
 * 5. HDFC Bank Message Parser
 */
class HdfcBankParser : BaseIndianBankParser() {
    override val bankName: String = "HDFC Bank"

    override fun canHandle(senderOrPackage: String, text: String): Boolean {
        val upper = (senderOrPackage + " " + text).uppercase(Locale.ROOT)
        return upper.contains("HDFCBK") || upper.contains("HDFC BANK") || upper.contains("HDFC")
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        if (isNonTransaction(combined)) return null

        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined)
        val dateMs = extractDateEpochMs(combined) ?: payload.postTimeEpochMs

        val lower = combined.lowercase(Locale.ROOT)
        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn") || lower.contains("paid") || lower.contains("spent")
        val isRefund = lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")
        val direction = if (isRefund || (isCredit && !isDebit)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")

        val txType = when {
            isRefund -> BankTransactionType.REFUND_REVERSAL
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            lower.contains("salary") -> BankTransactionType.SALARY_CREDIT
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val sanitized = combined.replace(Regex("(?i)(?:not you\\?|if not you|call\\s+\\d+|for any query|toll free).*"), "")
        val merchantPattern = if (direction == TransactionDirection.CREDIT) {
            Pattern.compile("(?i)(?:from|received from|by)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+upi|\\s+avl|\\s+ref|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile("(?i)(?:towards|to|at|paid to)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+upi|\\s+avl|\\s+ref|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        }
        val mMatcher = merchantPattern.matcher(sanitized)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "HDFC Payee" else "HDFC Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(80),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = dateMs,
            sourcePackage = payload.packageName
        )
    }
}

/**
 * 6. State Bank of India (SBI) Message Parser
 */
class SbiBankParser : BaseIndianBankParser() {
    override val bankName: String = "State Bank of India"

    override fun canHandle(senderOrPackage: String, text: String): Boolean {
        val upper = (senderOrPackage + " " + text).uppercase(Locale.ROOT)
        return upper.contains("SBIUPI") || upper.contains("SBINB") || upper.contains("SBIPSG") ||
                upper.contains("STATE BANK") || upper.contains("SBI")
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        if (isNonTransaction(combined)) return null

        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined)
        val dateMs = extractDateEpochMs(combined) ?: payload.postTimeEpochMs

        val lower = combined.lowercase(Locale.ROOT)
        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn") || lower.contains("paid") || lower.contains("spent")
        val isRefund = lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")
        val direction = if (isRefund || (isCredit && !isDebit)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")

        val txType = when {
            isRefund -> BankTransactionType.REFUND_REVERSAL
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            lower.contains("salary") -> BankTransactionType.SALARY_CREDIT
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val sanitized = combined.replace(Regex("(?i)(?:not you\\?|if not you|call\\s+\\d+|for any query|toll free).*"), "")
        val merchantPattern = if (direction == TransactionDirection.CREDIT) {
            Pattern.compile("(?i)(?:from|received from|by)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+ref|\\s+avl|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile("(?i)(?:transfer\\s+to|towards|to|at|paid to)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+ref|\\s+avl|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        }
        val mMatcher = merchantPattern.matcher(sanitized)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "SBI Payee" else "SBI Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(80),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = dateMs,
            sourcePackage = payload.packageName
        )
    }
}

/**
 * 7. ICICI Bank Message Parser
 */
class IciciBankParser : BaseIndianBankParser() {
    override val bankName: String = "ICICI Bank"

    override fun canHandle(senderOrPackage: String, text: String): Boolean {
        val upper = (senderOrPackage + " " + text).uppercase(Locale.ROOT)
        return upper.contains("ICICIB") || upper.contains("ICICI BANK") || upper.contains("ICICI")
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        if (isNonTransaction(combined)) return null

        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined)
        val dateMs = extractDateEpochMs(combined) ?: payload.postTimeEpochMs

        val lower = combined.lowercase(Locale.ROOT)
        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn") || lower.contains("paid") || lower.contains("spent")
        val isRefund = lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")
        val direction = if (isRefund || (isCredit && !isDebit)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")

        val txType = when {
            isRefund -> BankTransactionType.REFUND_REVERSAL
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val sanitized = combined.replace(Regex("(?i)(?:not you\\?|if not you|call\\s+\\d+|for any query|toll free).*"), "")
        val merchantPattern = if (direction == TransactionDirection.CREDIT) {
            Pattern.compile("(?i)(?:from|received from|by)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+ref|\\s+avl|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile("(?i)(?:towards|to|at|info:\\s*BIL\\*|paid to)\\s*([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+ref|\\s+avl|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        }
        val mMatcher = merchantPattern.matcher(sanitized)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "ICICI Payee" else "ICICI Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(80),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = dateMs,
            sourcePackage = payload.packageName
        )
    }
}

/**
 * 8. Axis Bank Message Parser
 */
class AxisBankParser : BaseIndianBankParser() {
    override val bankName: String = "Axis Bank"

    override fun canHandle(senderOrPackage: String, text: String): Boolean {
        val upper = (senderOrPackage + " " + text).uppercase(Locale.ROOT)
        return upper.contains("AXISBK") || upper.contains("AXIS BANK") || upper.contains("AXIS")
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        if (isNonTransaction(combined)) return null

        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined)
        val dateMs = extractDateEpochMs(combined) ?: payload.postTimeEpochMs

        val lower = combined.lowercase(Locale.ROOT)
        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn") || lower.contains("paid") || lower.contains("spent")
        val isRefund = lower.contains("refund") || lower.contains("reversed") || lower.contains("reversal")
        val direction = if (isRefund || (isCredit && !isDebit)) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")

        val txType = when {
            isRefund -> BankTransactionType.REFUND_REVERSAL
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val sanitized = combined.replace(Regex("(?i)(?:not you\\?|if not you|call\\s+\\d+|for any query|toll free).*"), "")
        val merchantPattern = if (direction == TransactionDirection.CREDIT) {
            Pattern.compile("(?i)(?:from|received from|by)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+ref|\\s+avl|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        } else {
            Pattern.compile("(?i)(?:towards|to|at|paid to)\\s+([A-Za-z0-9\\s.'&-]{2,80}?)(?:\\s+info\\s*:|\\s+on\\s+[0-9]|\\s+ref|\\s+avl|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        }
        val mMatcher = merchantPattern.matcher(sanitized)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "Axis Payee" else "Axis Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(80),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = dateMs,
            sourcePackage = payload.packageName
        )
    }
}

/**
 * 9. Extensible Registry for all Indian Bank Parsers
 */
object BankMessageParserRegistry {
    private val specializedParsers = listOf(
        KvbBankParser(),
        KotakBankParser(),
        IndianBankParser(),
        HdfcBankParser(),
        SbiBankParser(),
        IciciBankParser(),
        AxisBankParser()
    )
    private val genericParser = GenericBankParser()

    fun findParser(senderOrPackage: String, text: String): BankMessageParser? {
        val specialized = specializedParsers.firstOrNull { it.canHandle(senderOrPackage, text) }
        if (specialized != null) return specialized
        return if (genericParser.canHandle(senderOrPackage, text)) genericParser else null
    }

    fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val bankId = BankIdentifier.identifyBank(payload)
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}"

        val matchedParser = if (bankId.bankName != null) {
            specializedParsers.firstOrNull { it.bankName.equals(bankId.bankName, ignoreCase = true) }
        } else {
            specializedParsers.firstOrNull { it.canHandle(payload.packageName, combined) }
        }

        val candidate = matchedParser?.parse(payload)
            ?: genericParser.takeIf { it.canHandle(payload.packageName, combined) }?.parse(payload)
            ?: return null

        val finalConfidence = if (bankId.confidence > 0f) {
            (candidate.confidence * 0.6f + bankId.confidence * 0.4f).coerceIn(0.1f, 0.99f)
        } else {
            candidate.confidence
        }

        val requiresReview = candidate.reviewRequired || bankId.isAmbiguous || finalConfidence < 0.80f
        val reviewReason = when {
            bankId.isAmbiguous -> "Ambiguous originating bank detected from notification text"
            candidate.reviewReason != null -> candidate.reviewReason
            finalConfidence < 0.80f -> "Low parsing confidence"
            else -> null
        }

        return candidate.copy(
            bankName = bankId.bankName ?: candidate.bankName,
            confidence = finalConfidence,
            reviewRequired = requiresReview,
            reviewReason = reviewReason
        )
    }

    fun parse(text: String, packageNameOrSender: String = ""): ParsedBankMessage? {
        val payload = RawNotificationPayload(
            packageName = packageNameOrSender,
            title = "",
            text = text,
            postTimeEpochMs = System.currentTimeMillis()
        )
        return parse(payload)
    }
}
