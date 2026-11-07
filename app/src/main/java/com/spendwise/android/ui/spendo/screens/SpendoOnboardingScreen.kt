package com.spendwise.android.ui.spendo.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoMascot
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoTextField
import com.spendwise.android.ui.spendo.model.OnboardingBankDraft
import com.spendwise.android.ui.spendo.theme.SpendoColor
import java.util.Locale

@Composable
fun SpendoOnboardingScreen(
    onFinishOnboarding: (
        name: String,
        incomePaise: Long,
        frequency: String,
        dailyLimitPaise: Long?,
        banks: List<OnboardingBankDraft>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(1) }

    // Step 2: Personal Details
    var displayName by remember { mutableStateOf("") }
    var preferredCurrency by remember { mutableStateOf("INR") }

    // Step 3: Income
    var monthlyIncomeInput by remember { mutableStateOf("") }
    var incomeFrequency by remember { mutableStateOf("MONTHLY") }
    var additionalIncomeInput by remember { mutableStateOf("") }
    var isIncomeSkipped by remember { mutableStateOf(false) }

    // Step 4: Bank Accounts
    val bankDrafts = remember { mutableStateListOf<OnboardingBankDraft>() }
    var bankNameInput by remember { mutableStateOf("") }
    var nicknameInput by remember { mutableStateOf("") }
    var accountNumInput by remember { mutableStateOf("") }
    var openingBalInput by remember { mutableStateOf("") }
    var bankErrorMessage by remember { mutableStateOf<String?>(null) }

    // Step 5: Daily Spending Limit
    var dailyLimitInput by remember { mutableStateOf("2500") }
    var isDailyLimitSkipped by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        // Step Progress Indicator Bar (if past step 1)
        if (currentStep > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (currentStep > 1) currentStep-- },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SpendoColor.TextPrimary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..6).forEach { stepIndex ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (stepIndex == currentStep) 24.dp else 8.dp,
                                    height = 4.dp
                                )
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (stepIndex <= currentStep) SpendoColor.TextPrimary else SpendoColor.SurfaceVariant
                                )
                        )
                    }
                }

                DotMatrixText(
                    text = "STEP $currentStep/6",
                    fontSize = 11.sp,
                    color = SpendoColor.TextSecondary
                )
            }
        }

        // Main Step Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentStep) {
                1 -> StepWelcome(onStart = { currentStep = 2 })
                2 -> StepPersonalDetails(
                    name = displayName,
                    onNameChange = { displayName = it },
                    currency = preferredCurrency,
                    onCurrencyChange = { preferredCurrency = it },
                    onNext = { currentStep = 3 }
                )
                3 -> StepIncome(
                    incomeInput = monthlyIncomeInput,
                    onIncomeChange = { monthlyIncomeInput = it },
                    frequency = incomeFrequency,
                    onFrequencyChange = { incomeFrequency = it },
                    additionalIncome = additionalIncomeInput,
                    onAdditionalIncomeChange = { additionalIncomeInput = it },
                    onSkip = {
                        isIncomeSkipped = true
                        monthlyIncomeInput = ""
                        currentStep = 4
                    },
                    onNext = {
                        isIncomeSkipped = false
                        currentStep = 4
                    }
                )
                4 -> StepBankAccounts(
                    drafts = bankDrafts,
                    bankName = bankNameInput,
                    onBankNameChange = {
                        bankNameInput = it
                        bankErrorMessage = null
                    },
                    nickname = nicknameInput,
                    onNicknameChange = { nicknameInput = it },
                    accountNumber = accountNumInput,
                    onAccountNumberChange = {
                        accountNumInput = it.filter { ch -> ch.isDigit() }
                        bankErrorMessage = null
                    },
                    openingBalance = openingBalInput,
                    onOpeningBalanceChange = { openingBalInput = it.filter { ch -> ch.isDigit() } },
                    errorMessage = bankErrorMessage,
                    onAddDraft = {
                        val trimmedBank = bankNameInput.trim()
                        if (trimmedBank.isBlank()) {
                            bankErrorMessage = "Bank name cannot be blank."
                            return@StepBankAccounts
                        }
                        val digits = accountNumInput.filter { it.isDigit() }
                        val last4 = if (digits.length >= 4) digits.takeLast(4) else digits.padStart(4, '0')
                        val masked = "•••• $last4"

                        // Warn / check duplicate bank and last 4
                        val duplicate = bankDrafts.any {
                            it.bankName.equals(trimmedBank, ignoreCase = true) && it.maskedAccountNumber.endsWith(last4)
                        }
                        if (duplicate) {
                            bankErrorMessage = "Notice: An account with $trimmedBank ending in $last4 is already added."
                            return@StepBankAccounts
                        }

                        val balPaise = openingBalInput.toLongOrNull()?.let { it * 100L } ?: 0L
                        bankDrafts.add(
                            OnboardingBankDraft(
                                bankName = trimmedBank,
                                nickname = nicknameInput.trim().ifBlank { null },
                                maskedAccountNumber = masked,
                                initialBalancePaise = balPaise
                            )
                        )
                        bankNameInput = ""
                        nicknameInput = ""
                        accountNumInput = ""
                        openingBalInput = ""
                        bankErrorMessage = null
                    },
                    onRemoveDraft = { bankDrafts.remove(it) },
                    onNext = { currentStep = 5 }
                )
                5 -> StepDailyLimit(
                    limitInput = dailyLimitInput,
                    onLimitChange = { dailyLimitInput = it.filter { ch -> ch.isDigit() } },
                    onSkip = {
                        isDailyLimitSkipped = true
                        dailyLimitInput = ""
                        currentStep = 6
                    },
                    onNext = {
                        isDailyLimitSkipped = false
                        currentStep = 6
                    }
                )
                6 -> StepReviewFinish(
                    name = displayName,
                    currency = preferredCurrency,
                    incomePaise = if (isIncomeSkipped) 0L else (monthlyIncomeInput.toLongOrNull()?.let { it * 100L } ?: 0L),
                    frequency = incomeFrequency,
                    isIncomeSkipped = isIncomeSkipped,
                    banks = bankDrafts,
                    dailyLimitPaise = if (isDailyLimitSkipped) null else (dailyLimitInput.toLongOrNull()?.let { it * 100L }),
                    onEditStep = { step -> currentStep = step },
                    onFinish = {
                        val incomePaise = if (isIncomeSkipped) 0L else (monthlyIncomeInput.toLongOrNull()?.let { it * 100L } ?: 0L)
                        val limitPaise = if (isDailyLimitSkipped) null else (dailyLimitInput.toLongOrNull()?.let { it * 100L })
                        onFinishOnboarding(
                            displayName.ifBlank { "User" },
                            incomePaise,
                            incomeFrequency,
                            limitPaise,
                            bankDrafts.toList()
                        )
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1 — WELCOME
// -------------------------------------------------------------
@Composable
private fun StepWelcome(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            com.spendwise.android.ui.spendo.components.SpendoNeuralMonogram(
                size = 76.dp,
                showGlyphRing = true
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DotMatrixText(text = "WELCOME TO SPENDO", fontSize = 12.sp, color = SpendoColor.AccentRed)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your AI Money Agent",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Track expenses, master your daily safe limits, and build financial discipline with complete on-device privacy.",
                    fontSize = 14.sp,
                    color = SpendoColor.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Guarantees Card
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Shield, contentDescription = null, tint = SpendoColor.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text(text = "100% On-Device & Private", fontWeight = FontWeight.Bold, color = SpendoColor.TextPrimary, fontSize = 13.sp)
                            Text(text = "Financial parsing, ledger records, and calculations never leave your physical device.", fontSize = 11.sp, color = SpendoColor.TextSecondary)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = SpendoColor.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text(text = "Zero Banking Connections", fontWeight = FontWeight.Bold, color = SpendoColor.TextPrimary, fontSize = 13.sp)
                            Text(text = "No internet banking passwords, UPI credentials, or account linking required. Ever.", fontSize = 11.sp, color = SpendoColor.TextSecondary)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Speed, contentDescription = null, tint = SpendoColor.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text(text = "Live Widget & Voice Entry", fontWeight = FontWeight.Bold, color = SpendoColor.TextPrimary, fontSize = 13.sp)
                            Text(text = "Track today's spending progress, streak counter, and log expenses directly from your home screen.", fontSize = 11.sp, color = SpendoColor.TextSecondary)
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            SpendoPrimaryButton(
                text = "Get Started",
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 2 — PERSONAL DETAILS
// -------------------------------------------------------------
@Composable
private fun StepPersonalDetails(
    name: String,
    onNameChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column {
                DotMatrixText(text = "PERSONAL PREFERENCES", fontSize = 11.sp, color = SpendoColor.SuccessGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "What should we call you?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your preferred display name. No email, phone number, or unnecessary personal info is collected.",
                    fontSize = 13.sp,
                    color = SpendoColor.TextSecondary,
                    lineHeight = 18.sp
                )
            }

            SpendoTextField(
                value = name,
                onValueChange = onNameChange,
                label = "Your Name / Preferred Nickname",
                placeholder = "e.g. Alex"
            )

            // Preferred Currency
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PREFERRED CURRENCY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SpendoColor.TextSecondary,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("INR" to "₹ Indian Rupee (INR)", "USD" to "$ US Dollar (USD)").forEach { (code, label) ->
                        val isSelected = currency == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) SpendoColor.TextPrimary else SpendoColor.Surface)
                                .border(1.dp, if (isSelected) SpendoColor.TextPrimary else SpendoColor.Divider, RoundedCornerShape(12.dp))
                                .clickable { onCurrencyChange(code) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) SpendoColor.Background else SpendoColor.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = 32.dp)) {
            SpendoPrimaryButton(
                text = "Continue",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 3 — INCOME & FINANCIAL PREFERENCES
// -------------------------------------------------------------
@Composable
private fun StepIncome(
    incomeInput: String,
    onIncomeChange: (String) -> Unit,
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    additionalIncome: String,
    onAdditionalIncomeChange: (String) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                DotMatrixText(text = "INCOME PREFERENCES", fontSize = 11.sp, color = SpendoColor.SuccessGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Monthly Salary & Income",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Declare your recurring earnings to calculate savings rate and safe spend limits. This information is private and optional.",
                    fontSize = 13.sp,
                    color = SpendoColor.TextSecondary,
                    lineHeight = 18.sp
                )
            }

            SpendoTextField(
                value = incomeInput,
                onValueChange = onIncomeChange,
                label = "Primary Monthly Income (₹)",
                placeholder = "e.g. 75000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Frequency
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PAYMENT FREQUENCY",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SpendoColor.TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MONTHLY" to "Monthly", "BIWEEKLY" to "Bi-Weekly", "WEEKLY" to "Weekly").forEach { (freqKey, label) ->
                        val isSelected = frequency == freqKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) SpendoColor.TextPrimary else SpendoColor.Surface)
                                .border(1.dp, if (isSelected) SpendoColor.TextPrimary else SpendoColor.Divider, RoundedCornerShape(12.dp))
                                .clickable { onFrequencyChange(freqKey) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) SpendoColor.Background else SpendoColor.TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            SpendoTextField(
                value = additionalIncome,
                onValueChange = onAdditionalIncomeChange,
                label = "Additional Income / Freelance (Optional)",
                placeholder = "e.g. 15000",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Non-Negotiable Privacy Callout
            SpendoDarkCard(contentPadding = 14.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = SpendoColor.SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Salary privacy: Your income is private and stored only within your on-device settings. It is NEVER shown in the home-screen widget, and no unverified bank transactions will be generated.",
                        fontSize = 11.sp,
                        color = SpendoColor.TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpendoPrimaryButton(
                text = "Continue",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
            SpendoOutlinedButton(
                text = "Skip Income Details",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 4 — BANK ACCOUNTS
// -------------------------------------------------------------
@Composable
private fun StepBankAccounts(
    drafts: List<OnboardingBankDraft>,
    bankName: String,
    onBankNameChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    accountNumber: String,
    onAccountNumberChange: (String) -> Unit,
    openingBalance: String,
    onOpeningBalanceChange: (String) -> Unit,
    errorMessage: String?,
    onAddDraft: () -> Unit,
    onRemoveDraft: (OnboardingBankDraft) -> Unit,
    onNext: () -> Unit
) {
    val quickBanks = listOf("HDFC", "SBI", "ICICI", "Axis", "Kotak", "KVB")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                DotMatrixText(text = "PAYMENT ACCOUNTS", fontSize = 11.sp, color = SpendoColor.SuccessGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add Bank Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Distinguish accounts by bank name and the last 4 digits of each account. Multiple accounts from the same bank remain completely separate.",
                    fontSize = 13.sp,
                    color = SpendoColor.TextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Quick Banks Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(quickBanks) { bank ->
                    FilterChip(
                        selected = bankName.equals(bank, ignoreCase = true),
                        onClick = { onBankNameChange(bank) },
                        label = { Text(bank, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpendoColor.TextPrimary,
                            selectedLabelColor = SpendoColor.Background,
                            containerColor = SpendoColor.Surface,
                            labelColor = SpendoColor.TextPrimary
                        )
                    )
                }
            }

            SpendoTextField(
                value = bankName,
                onValueChange = onBankNameChange,
                label = "Bank Name",
                placeholder = "e.g. KVB, HDFC, SBI"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SpendoTextField(
                        value = accountNumber,
                        onValueChange = onAccountNumberChange,
                        label = "Last 4 Digits or Account #",
                        placeholder = "e.g. 9362",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpendoTextField(
                        value = openingBalance,
                        onValueChange = onOpeningBalanceChange,
                        label = "Opening Bal (₹)",
                        placeholder = "e.g. 15000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            SpendoTextField(
                value = nickname,
                onValueChange = onNicknameChange,
                label = "Account Nickname (Optional)",
                placeholder = "e.g. Salary Account, Emergency Fund"
            )

            if (errorMessage != null) {
                Text(
                    text = "⚠️ $errorMessage",
                    color = SpendoColor.WarningAmber,
                    fontSize = 12.sp
                )
            }

            SpendoOutlinedButton(
                text = "+ Add This Account",
                onClick = onAddDraft,
                modifier = Modifier.fillMaxWidth()
            )

            // Added Accounts Summary List
            if (drafts.isNotEmpty()) {
                Text(
                    text = "ADDED ACCOUNTS (${drafts.size})",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SpendoColor.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (draft in drafts) {
                        SpendoCard(contentPadding = 12.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalance,
                                        contentDescription = null,
                                        tint = SpendoColor.TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${draft.bankName} ${draft.maskedAccountNumber}",
                                            fontWeight = FontWeight.Bold,
                                            color = SpendoColor.TextPrimary,
                                            fontSize = 13.sp
                                        )
                                        if (draft.nickname != null) {
                                            Text(text = draft.nickname, fontSize = 11.sp, color = SpendoColor.TextSecondary)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onRemoveDraft(draft) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = SpendoColor.AccentRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpendoPrimaryButton(
                text = if (drafts.isEmpty()) "Continue Without Accounts" else "Continue (${drafts.size} Accounts)",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 5 — DAILY SPENDING LIMIT
// -------------------------------------------------------------
@Composable
private fun StepDailyLimit(
    limitInput: String,
    onLimitChange: (String) -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    val presets = listOf("1000", "2500", "5000", "10000")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column {
                DotMatrixText(text = "DAILY BUDGET CEILING", fontSize = 11.sp, color = SpendoColor.SuccessGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Daily Spending Limit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Configure your daily budget ceiling. The home-screen widget uses this target to display your spending progress and track your disciplined streaks.",
                    fontSize = 13.sp,
                    color = SpendoColor.TextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Presets
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    val isSelected = limitInput == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SpendoColor.TextPrimary else SpendoColor.Surface)
                            .border(1.dp, if (isSelected) SpendoColor.TextPrimary else SpendoColor.Divider, RoundedCornerShape(12.dp))
                            .clickable { onLimitChange(preset) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "₹$preset",
                            color = if (isSelected) SpendoColor.Background else SpendoColor.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            SpendoTextField(
                value = limitInput,
                onValueChange = onLimitChange,
                label = "Target Daily Limit in ₹",
                placeholder = "e.g. 2500",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Live Widget Preview Box
            SpendoDarkCard(contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DotMatrixText(text = "HOME SCREEN WIDGET PREVIEW", fontSize = 10.sp, color = SpendoColor.SuccessGreen)
                    val limitVal = limitInput.toLongOrNull() ?: 2500L
                    Text(
                        text = "₹0.00 spent today",
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.TextPrimary,
                        fontSize = 18.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SpendoColor.SurfaceVariant)
                    )
                    Text(
                        text = "₹%,.0f left of ₹%,.0f safe daily ceiling".format(Locale.ENGLISH, limitVal.toDouble(), limitVal.toDouble()),
                        fontSize = 11.sp,
                        color = SpendoColor.TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpendoPrimaryButton(
                text = "Continue",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
            SpendoOutlinedButton(
                text = "Skip Limit (Configure Later)",
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 6 — REVIEW & FINISH
// -------------------------------------------------------------
@Composable
private fun StepReviewFinish(
    name: String,
    currency: String,
    incomePaise: Long,
    frequency: String,
    isIncomeSkipped: Boolean,
    banks: List<OnboardingBankDraft>,
    dailyLimitPaise: Long?,
    onEditStep: (Int) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column {
                DotMatrixText(text = "REVIEW SETUP", fontSize = 11.sp, color = SpendoColor.SuccessGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You're All Set!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Verify your preferences below. You can update these anytime from the app Settings.",
                    fontSize = 13.sp,
                    color = SpendoColor.TextSecondary,
                    lineHeight = 18.sp
                )
            }

            // Profile Card
            SpendoCard(contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        DotMatrixText(text = "PERSONAL PROFILE", fontSize = 9.sp, color = SpendoColor.TextSecondary)
                        Text(text = name.ifBlank { "User" }, fontWeight = FontWeight.Bold, color = SpendoColor.TextPrimary, fontSize = 14.sp)
                        Text(text = "Currency: $currency", fontSize = 12.sp, color = SpendoColor.TextSecondary)
                    }
                    TextButton(onClick = { onEditStep(2) }) {
                        Text("Edit", color = SpendoColor.SuccessGreen)
                    }
                }
            }

            // Income Card
            SpendoCard(contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        DotMatrixText(text = "SALARY & INCOME (PRIVATE)", fontSize = 9.sp, color = SpendoColor.TextSecondary)
                        if (isIncomeSkipped || incomePaise <= 0L) {
                            Text(text = "Skipped (Not configured)", fontSize = 13.sp, color = SpendoColor.TextMuted)
                        } else {
                            Text(
                                text = "₹%,.2f (%s)".format(Locale.ENGLISH, incomePaise / 100.0, frequency.lowercase()),
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                    TextButton(onClick = { onEditStep(3) }) {
                        Text("Edit", color = SpendoColor.TextPrimary)
                    }
                }
            }

            // Bank Accounts Card
            SpendoCard(contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        DotMatrixText(text = "BANK ACCOUNTS", fontSize = 9.sp, color = SpendoColor.TextSecondary)
                        if (banks.isEmpty()) {
                            Text(text = "None added (Manual tracking mode)", fontSize = 13.sp, color = SpendoColor.TextMuted)
                        } else {
                            Text(
                                text = "${banks.size} Account(s) Configured",
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextPrimary,
                                fontSize = 14.sp
                            )
                            val summary = banks.joinToString(" • ") { "${it.bankName} ${it.maskedAccountNumber}" }
                            Text(text = summary, fontSize = 11.sp, color = SpendoColor.TextSecondary, maxLines = 2)
                        }
                    }
                    TextButton(onClick = { onEditStep(4) }) {
                        Text("Edit", color = SpendoColor.TextPrimary)
                    }
                }
            }

            // Daily Limit Card
            SpendoCard(contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        DotMatrixText(text = "DAILY SPENDING CEILING", fontSize = 9.sp, color = SpendoColor.TextSecondary)
                        if (dailyLimitPaise != null && dailyLimitPaise > 0L) {
                            Text(
                                text = "₹%,.2f / day".format(Locale.ENGLISH, dailyLimitPaise / 100.0),
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextPrimary,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(text = "Skipped", fontSize = 13.sp, color = SpendoColor.TextMuted)
                        }
                    }
                    TextButton(onClick = { onEditStep(5) }) {
                        Text("Edit", color = SpendoColor.TextPrimary)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            SpendoPrimaryButton(
                text = "✓ Finish Setup & Enter Spendo",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
