package com.spendwise.android.domain.banking

import com.spendwise.android.domain.model.TransactionDirection
import java.security.MessageDigest

/**
 * Granular classification of Indian banking transactions.
 */
enum class BankTransactionType(val displayName: String, val isCashMovement: Boolean = false) {
    UPI_DEBIT("UPI Payment"),
    UPI_CREDIT("UPI Received"),
    IMPS_TRANSFER("IMPS Transfer"),
    NEFT_TRANSFER("NEFT Transfer"),
    RTGS_TRANSFER("RTGS Transfer"),
    CARD_POS_PURCHASE("Debit Card POS"),
    ATM_WITHDRAWAL("ATM Cash Withdrawal", isCashMovement = true),
    ATM_DEPOSIT("ATM Cash Deposit", isCashMovement = true),
    INTERNAL_TRANSFER("Internal Account Transfer"),
    SALARY_CREDIT("Salary Credit"),
    REFUND_REVERSAL("Refund / Reversal"),
    BANK_CHARGE("Bank Service Charge"),
    UNKNOWN("Bank Transaction")
}

/**
 * Normalized result of parsing an official Indian bank notification or SMS.
 * All monetary values use strictly Integer Paise (Long).
 */
data class ParsedBankMessage(
    val amountPaise: Long,
    val direction: TransactionDirection,
    val transactionType: BankTransactionType,
    val bankName: String,
    val accountSuffix: String?,
    val merchantOrPayee: String,
    val referenceNumber: String?, // UTR, RRN, or Bank Ref ID
    val reportedBalancePaise: Long? = null, // Snapshot of available balance
    val isAtmCashMovement: Boolean = false,
    val confidence: Float = 0.95f,
    val reviewRequired: Boolean = false,
    val reviewReason: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val sourcePackage: String = ""
) {
    /**
     * Computes a resilient deduplication fingerprint.
     * When bank-issued references (UTR/RRN) exist, identical references indicate duplicate delivery,
     * while distinct references guarantee separate transactions (even for two ₹1 transfers seconds apart).
     */
    val dedupeFingerprint: String
        get() {
            val key = if (!referenceNumber.isNullOrBlank()) {
                // Strongest identity: Bank Name + Account + Unique Bank Ref Number
                "REF:$bankName:$accountSuffix:$referenceNumber"
            } else {
                // Fallback identity: Bank + Suffix + Amount + Direction + Merchant + Timestamp
                "FALLBACK:$bankName:$accountSuffix:$amountPaise:${direction.name}:$merchantOrPayee:$timestampEpochMs"
            }
            return sha256(key)
        }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
