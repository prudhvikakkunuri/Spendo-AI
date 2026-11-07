package com.spendwise.android.ui.spendo.model

/**
 * Transient draft model used during first-time onboarding for account creation.
 * Amounts are strictly Long integer paise.
 */
data class OnboardingBankDraft(
    val bankName: String,
    val nickname: String? = null,
    val maskedAccountNumber: String = "",
    val initialBalancePaise: Long = 0L,
    val accountType: String = "SAVINGS"
) {
    val last4Digits: String
        get() = maskedAccountNumber.filter { it.isDigit() }.takeLast(4)
}
