package com.spendwise.android.domain.banking

import com.spendwise.android.data.db.dao.BankAccountDao
import com.spendwise.android.data.db.entity.BankAccountEntity
import java.util.Locale

/**
 * Matching status for associating an incoming bank notification with locally saved accounts.
 * Strictly enforces review-first behavior and eliminates guessing.
 */
enum class AccountMatchStatus {
    EXACT_MATCH,
    INFERRED_SINGLE_ACCOUNT,
    AMBIGUOUS_MULTI_MATCH,
    UNKNOWN_SUFFIX_UNLINKED,
    CONFLICTING_MATCH,
    UNKNOWN_BANK_MATCH,
    SUFFIX_ONLY_MATCH, // Retained for backward compatibility as alias to UNKNOWN_BANK_MATCH
    NO_MATCH_UNLINKED
}

data class AccountMatchResult(
    val status: AccountMatchStatus,
    val matchedAccount: BankAccountEntity?,
    val candidateAccounts: List<BankAccountEntity>,
    val explanation: String,
    val extractedSuffix: String? = null,
    val detectedBank: String? = null,
    val matchConfidence: Float = 0f
) {
    /**
     * Strictly true ONLY for unambiguous EXACT_MATCH with a single verified account record.
     * All ambiguous, inferred, conflicting, or unlinked matches evaluate to false.
     */
    val isAutoAssignable: Boolean
        get() = status == AccountMatchStatus.EXACT_MATCH && matchedAccount != null
}

/**
 * Robust bank and account reconciliation engine.
 * Combines bank identification and 4-digit account suffix detection to associate transactions
 * with exact saved accounts, supporting multiple accounts at the same bank.
 */
class BankAccountMatcher(
    private val bankAccountDao: BankAccountDao
) {

    suspend fun matchAccount(
        bankName: String?,
        accountSuffix: String?
    ): AccountMatchResult {
        val allAccounts = bankAccountDao.getAllBankAccountsDirect()

        val cleanSuffix = accountSuffix?.filter { it.isDigit() }?.takeLast(4)?.ifBlank { null }
        val cleanBank = bankName?.trim()?.ifBlank { null }

        if (allAccounts.isEmpty()) {
            return AccountMatchResult(
                status = AccountMatchStatus.NO_MATCH_UNLINKED,
                matchedAccount = null,
                candidateAccounts = emptyList(),
                explanation = "No bank accounts are registered in Spendo. Please link your account.",
                extractedSuffix = cleanSuffix,
                detectedBank = cleanBank,
                matchConfidence = 0.0f
            )
        }

        val matchingBankAccounts = if (cleanBank != null) {
            allAccounts.filter { isSameBank(it.bankName, cleanBank) }
        } else emptyList()

        val nonMatchingBankAccounts = if (cleanBank != null) {
            allAccounts.filter { !isSameBank(it.bankName, cleanBank) }
        } else allAccounts

        // -------------------------------------------------------------
        // STAGE A: Bank Identified
        // -------------------------------------------------------------
        if (cleanBank != null && matchingBankAccounts.isNotEmpty()) {
            if (cleanSuffix != null) {
                // Rule 1: Exact Bank + Exact Suffix
                val exactMatches = matchingBankAccounts.filter { extractSavedAccountSuffix(it) == cleanSuffix }
                when {
                    exactMatches.size == 1 -> {
                        return AccountMatchResult(
                            status = AccountMatchStatus.EXACT_MATCH,
                            matchedAccount = exactMatches.first(),
                            candidateAccounts = exactMatches,
                            explanation = "Exact match on bank ($cleanBank) and account suffix (•••• $cleanSuffix)",
                            extractedSuffix = cleanSuffix,
                            detectedBank = cleanBank,
                            matchConfidence = 1.0f
                        )
                    }
                    exactMatches.size > 1 -> {
                        return AccountMatchResult(
                            status = AccountMatchStatus.AMBIGUOUS_MULTI_MATCH,
                            matchedAccount = null,
                            candidateAccounts = exactMatches,
                            explanation = "Multiple saved accounts match $cleanBank •••• $cleanSuffix. Please select intended account.",
                            extractedSuffix = cleanSuffix,
                            detectedBank = cleanBank,
                            matchConfidence = 0.5f
                        )
                    }
                    else -> {
                        // Check if suffix matches an account at another bank (Conflicting match)
                        val conflictingMatches = nonMatchingBankAccounts.filter { extractSavedAccountSuffix(it) == cleanSuffix }
                        if (conflictingMatches.isNotEmpty()) {
                            // Rule 5: Conflicting bank and suffix -> Require manual selection
                            return AccountMatchResult(
                                status = AccountMatchStatus.CONFLICTING_MATCH,
                                matchedAccount = null,
                                candidateAccounts = (matchingBankAccounts + conflictingMatches).distinctBy { it.id },
                                explanation = "Conflicting bank ($cleanBank) and suffix (•••• $cleanSuffix). Manual selection required.",
                                extractedSuffix = cleanSuffix,
                                detectedBank = cleanBank,
                                matchConfidence = 0.3f
                            )
                        } else {
                            // Rule 4: Exact bank, Unknown suffix -> Mark account unlinked
                            return AccountMatchResult(
                                status = AccountMatchStatus.UNKNOWN_SUFFIX_UNLINKED,
                                matchedAccount = null,
                                candidateAccounts = matchingBankAccounts,
                                explanation = "Bank matches $cleanBank, but suffix •••• $cleanSuffix is not registered in Spendo.",
                                extractedSuffix = cleanSuffix,
                                detectedBank = cleanBank,
                                matchConfidence = 0.2f
                            )
                        }
                    }
                }
            } else {
                // Suffix missing in alert
                return when {
                    matchingBankAccounts.size == 1 -> {
                        // Rule 2: Exact bank, Missing suffix, one saved account -> Mark as inferred; allow user review
                        AccountMatchResult(
                            status = AccountMatchStatus.INFERRED_SINGLE_ACCOUNT,
                            matchedAccount = null, // Strictly requires user confirmation; never silently auto-assigns
                            candidateAccounts = matchingBankAccounts,
                            explanation = "Notification omitted account suffix. Inferred single account ${matchingBankAccounts.first().bankName} (${matchingBankAccounts.first().maskedAccountNumber}). Please confirm.",
                            extractedSuffix = null,
                            detectedBank = cleanBank,
                            matchConfidence = 0.7f
                        )
                    }
                    else -> {
                        // Rule 3: Exact bank, Missing suffix, multiple accounts -> Ask user to select
                        AccountMatchResult(
                            status = AccountMatchStatus.AMBIGUOUS_MULTI_MATCH,
                            matchedAccount = null,
                            candidateAccounts = matchingBankAccounts,
                            explanation = "Multiple accounts exist for $cleanBank. Suffix missing from alert; please choose account.",
                            extractedSuffix = null,
                            detectedBank = cleanBank,
                            matchConfidence = 0.4f
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // STAGE B: Unknown or Unregistered Bank
        // -------------------------------------------------------------
        if (cleanSuffix != null) {
            val suffixMatches = allAccounts.filter { extractSavedAccountSuffix(it) == cleanSuffix }
            return when {
                suffixMatches.size == 1 -> {
                    // Rule 6: Unknown bank, Exact suffix -> Require additional bank verification
                    AccountMatchResult(
                        status = AccountMatchStatus.UNKNOWN_BANK_MATCH,
                        matchedAccount = null,
                        candidateAccounts = suffixMatches,
                        explanation = "Account ending •••• $cleanSuffix matched, but bank is unverified. Please confirm account.",
                        extractedSuffix = cleanSuffix,
                        detectedBank = cleanBank,
                        matchConfidence = 0.6f
                    )
                }
                suffixMatches.size > 1 -> {
                    AccountMatchResult(
                        status = AccountMatchStatus.AMBIGUOUS_MULTI_MATCH,
                        matchedAccount = null,
                        candidateAccounts = suffixMatches,
                        explanation = "Multiple saved accounts end with •••• $cleanSuffix across different banks. Please select.",
                        extractedSuffix = cleanSuffix,
                        detectedBank = cleanBank,
                        matchConfidence = 0.4f
                    )
                }
                else -> {
                    AccountMatchResult(
                        status = AccountMatchStatus.NO_MATCH_UNLINKED,
                        matchedAccount = null,
                        candidateAccounts = emptyList(),
                        explanation = "Account suffix •••• $cleanSuffix does not match any saved account.",
                        extractedSuffix = cleanSuffix,
                        detectedBank = cleanBank,
                        matchConfidence = 0.1f
                    )
                }
            }
        }

        // Rule 7: Unknown bank, Missing suffix -> Manual account selection
        return AccountMatchResult(
            status = AccountMatchStatus.NO_MATCH_UNLINKED,
            matchedAccount = null,
            candidateAccounts = emptyList(),
            explanation = "Unable to determine bank or account suffix from notification. Manual account selection required.",
            extractedSuffix = null,
            detectedBank = cleanBank,
            matchConfidence = 0.0f
        )
    }

    companion object {
        fun extractSavedAccountSuffix(account: BankAccountEntity): String? {
            val digits = account.maskedAccountNumber.filter { it.isDigit() }
            return if (digits.length >= 4) digits.takeLast(4) else digits.ifEmpty { null }
        }

        fun isSameBank(bankA: String?, bankB: String?): Boolean {
            if (bankA.isNullOrBlank() || bankB.isNullOrBlank()) return false
            val a = bankA.trim().lowercase(Locale.ROOT)
            val b = bankB.trim().lowercase(Locale.ROOT)
            if (a == b) return true
            if (a.contains(b) || b.contains(a)) return true

            val isKvbA = a.contains("kvb") || a.contains("karur vysya")
            val isKvbB = b.contains("kvb") || b.contains("karur vysya")
            if (isKvbA && isKvbB) return true

            val isKotakA = a.contains("kotak")
            val isKotakB = b.contains("kotak")
            if (isKotakA && isKotakB) return true

            val isSbiA = a.contains("sbi") || a.contains("state bank")
            val isSbiB = b.contains("sbi") || b.contains("state bank")
            if (isSbiA && isSbiB) return true

            val isHdfcA = a.contains("hdfc")
            val isHdfcB = b.contains("hdfc")
            if (isHdfcA && isHdfcB) return true

            val isIciciA = a.contains("icici")
            val isIciciB = b.contains("icici")
            if (isIciciA && isIciciB) return true

            val isAxisA = a.contains("axis")
            val isAxisB = b.contains("axis")
            if (isAxisA && isAxisB) return true

            val isIndianA = a.contains("indian bank") || a.contains("indbnk")
            val isIndianB = b.contains("indian bank") || b.contains("indbnk")
            if (isIndianA && isIndianB) return true

            return false
        }
    }
}
