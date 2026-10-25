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
            "(?:debited\\s+(?:for|by|with)?|spent|sent|withdrawn|(?:cash\\s+)?withdrawal\\s+(?:of|at|from)?|credited\\s+(?:for|by|with)?|received|purchase\\s+of)\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val raw = matcher.group(1) ?: return null
            return parseToPaise(raw)
        }

        // Secondary fallback pattern for leading currency
        val fallbackPattern = Pattern.compile(
            "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|credited|withdrawn|spent|sent)",
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
            Pattern.compile("(?i)(?:upi\\s*ref(?:\\s*no)?|rrn|ref\\s*no|txn\\s*id|utr)[:\\s/]+([A-Za-z0-9]{6,25})"),
            Pattern.compile("(?i)UPI/([0-9]{10,20})"),
            Pattern.compile("(?i)Ref[:\\s]+([A-Za-z0-9]{6,25})")
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
        val pattern = Pattern.compile(
            "(?:a/c|acct?|account|card)?\\s*(?:no\\.?)?\\s*(?:[*xX]{2,}|ending\\s+in\\s+)?([0-9]{4})",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
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
        val lower = combined.lowercase(Locale.ROOT)

        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined) ?: "4892"

        val isDebit = lower.contains("debited") || lower.contains("withdrawn") || lower.contains("spent")
        val isCredit = lower.contains("credited") || lower.contains("deposited") || lower.contains("received")
        val direction = if (isCredit && !isDebit) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")
        val isSalary = lower.contains("salary")

        val txType = when {
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            isSalary -> BankTransactionType.SALARY_CREDIT
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        // Extract merchant/recipient
        val merchantPattern = Pattern.compile("(?i)(?:to|at|by)\\s+([A-Za-z0-9\\s.'&-]{2,30}?)(?:\\s+on|\\s+by|\\s+avl|\\s+ref|\\.|$)", Pattern.CASE_INSENSITIVE)
        val mMatcher = merchantPattern.matcher(combined)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "Indian Bank Payee" else "Indian Bank Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(35),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = payload.postTimeEpochMs,
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
        val lower = combined.lowercase(Locale.ROOT)

        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined) ?: "1920"

        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn")
        val direction = if (isCredit && !isDebit) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")

        val txType = when {
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("salary") -> BankTransactionType.SALARY_CREDIT
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val merchantPattern = Pattern.compile("(?i)(?:to|info:?)\\s+([A-Za-z0-9\\s.'&-]{2,30}?)(?:\\s+on|\\s+upi|\\s+avl|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        val mMatcher = merchantPattern.matcher(combined)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "Kotak Payee" else "Kotak Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(35),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = payload.postTimeEpochMs,
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
        return upper.contains("KVBANK") || upper.contains("KVB") || upper.contains("KARUR VYSYA")
    }

    override fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}".trim()
        val lower = combined.lowercase(Locale.ROOT)

        val amountPaise = extractAmountPaise(combined) ?: return null
        val reportedBal = extractAvailableBalancePaise(combined)
        val refNo = extractReferenceNumber(combined)
        val acSuffix = extractAccountSuffix(combined) ?: "7731"

        val isCredit = lower.contains("credited") || lower.contains("received")
        val isDebit = lower.contains("debited") || lower.contains("sent") || lower.contains("withdrawn")
        val direction = if (isCredit && !isDebit) TransactionDirection.CREDIT else TransactionDirection.DEBIT

        val isAtm = lower.contains("atm") || lower.contains("cash withdrawal")
        val isUpi = lower.contains("upi")

        val txType = when {
            isAtm -> if (direction == TransactionDirection.DEBIT) BankTransactionType.ATM_WITHDRAWAL else BankTransactionType.ATM_DEPOSIT
            isUpi -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
            lower.contains("neft") -> BankTransactionType.NEFT_TRANSFER
            lower.contains("imps") -> BankTransactionType.IMPS_TRANSFER
            else -> if (direction == TransactionDirection.DEBIT) BankTransactionType.UPI_DEBIT else BankTransactionType.UPI_CREDIT
        }

        val merchantPattern = Pattern.compile("(?i)(?:info:?|towards|to)\\s+([A-Za-z0-9\\s.'&-]{2,30}?)(?:\\s+on|\\s+ref|\\s+clear|\\s+bal|\\.|$)", Pattern.CASE_INSENSITIVE)
        val mMatcher = merchantPattern.matcher(combined)
        val merchant = if (mMatcher.find()) mMatcher.group(1)?.trim() ?: "KVB Payee" else "KVB Payee"

        return ParsedBankMessage(
            amountPaise = amountPaise,
            direction = direction,
            transactionType = txType,
            bankName = bankName,
            accountSuffix = acSuffix,
            merchantOrPayee = merchant.take(35),
            referenceNumber = refNo,
            reportedBalancePaise = reportedBal,
            isAtmCashMovement = txType.isCashMovement,
            confidence = if (refNo != null) 0.98f else 0.90f,
            timestampEpochMs = payload.postTimeEpochMs,
            sourcePackage = payload.packageName
        )
    }
}

/**
 * 4. Extensible Registry for all Indian Bank Parsers
 */
object BankMessageParserRegistry {
    private val parsers = listOf(
        IndianBankParser(),
        KotakBankParser(),
        KvbBankParser()
    )

    fun findParser(senderOrPackage: String, text: String): BankMessageParser? {
        return parsers.firstOrNull { it.canHandle(senderOrPackage, text) }
    }

    fun parse(payload: RawNotificationPayload): ParsedBankMessage? {
        val combined = "${payload.title.orEmpty()} ${payload.text.orEmpty()} ${payload.bigText.orEmpty()}"
        val parser = findParser(payload.packageName, combined)
        return parser?.parse(payload)
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
