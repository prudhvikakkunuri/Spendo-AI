package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixAmount
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoPasswordField
import com.spendwise.android.ui.spendo.components.SpendoTextField
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.BalanceSource
import com.spendwise.android.ui.spendo.model.SpendoBank
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoBanksScreen(
    uiState: SpendoUiState,
    onToggleUnlockDialog: (Boolean) -> Unit,
    onVerifyAndUnlock: (password: String, onSuccess: () -> Unit, onError: () -> Unit) -> Unit,
    onHideBalances: () -> Unit,
    onAddBank: (name: String, type: String, initialPaise: Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddBankDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Banks & Accounts",
            categoryBadge = "PAYMENT VAULT",
            onNavigateBack = onNavigateBack,
            actions = {
                IconButton(
                    onClick = {
                        if (uiState.isBalancesRevealed) {
                            onHideBalances()
                        } else {
                            onToggleUnlockDialog(true)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (uiState.isBalancesRevealed) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (uiState.isBalancesRevealed) "Conceal Balances" else "Reveal Balances",
                        tint = if (uiState.isBalancesRevealed) SpendoColor.AccentRed else SpendoColor.TextPrimary
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Privacy Disclaimer Callout
            item {
                SpendoCard(contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Shield,
                                contentDescription = null,
                                tint = SpendoColor.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            DotMatrixText(text = "Privacy Notice", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                            Text(
                                text = "Account balances are locally tracked on your device. Spendo does not connect to bank accounts or access financial APIs.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }
                    }
                }
            }

            // Total Net Worth Summary Card
            item {
                val totalBalancePaise = uiState.banks.sumOf { it.balancePaise }
                SpendoDarkCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DotGridBadge(
                                text = "${uiState.banks.size} Linked Accounts",
                                indicatorColor = SpendoColor.SuccessGreen,
                                containerColor = SpendoColor.DarkCardContainer,
                                textColor = SpendoColor.CardDarkText
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (uiState.isBalancesRevealed) {
                                            onHideBalances()
                                        } else {
                                            onToggleUnlockDialog(true)
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isBalancesRevealed) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = if (uiState.isBalancesRevealed) "Conceal" else "Unlock",
                                    tint = if (uiState.isBalancesRevealed) SpendoColor.SuccessGreen else SpendoColor.CardDarkTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (uiState.isBalancesRevealed) "UNLOCKED" else "CONCEALED",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isBalancesRevealed) SpendoColor.SuccessGreen else SpendoColor.CardDarkTextSecondary
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotMatrixText(
                                text = "Aggregated Balance • Locally Tracked Estimate",
                                fontSize = 10.sp,
                                color = SpendoColor.CardDarkTextSecondary
                            )
                            if (uiState.isBalancesRevealed) {
                                DotMatrixAmount(
                                    amountText = "%,.2f".format(totalBalancePaise / 100.0),
                                    fontSize = 32.sp,
                                    color = SpendoColor.CardDarkText
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.clickable { onToggleUnlockDialog(true) }
                                ) {
                                    Text(
                                        text = "₹ • • • • • •",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        letterSpacing = 2.sp,
                                        color = SpendoColor.CardDarkText
                                    )
                                    Text(
                                        text = "Tap eye to reveal",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = SpendoColor.CardDarkTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bank Accounts List
            items(uiState.banks, key = { it.id }) { bank ->
                SpendoCard(contentPadding = 18.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SpendoColor.SurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalance,
                                        contentDescription = null,
                                        tint = SpendoColor.TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = bank.bankName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SpendoColor.TextPrimary
                                    )
                                    Text(
                                        text = "${bank.accountType} • ${bank.maskedAccountNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SpendoColor.TextSecondary
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (uiState.isBalancesRevealed) {
                                    Text(
                                        text = bank.formattedBalance,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SpendoColor.TextPrimary
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable { onToggleUnlockDialog(true) }
                                    ) {
                                        Text(
                                            text = "₹••••••",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            letterSpacing = 1.sp,
                                            color = SpendoColor.TextMuted
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.VisibilityOff,
                                            contentDescription = "Hidden",
                                            tint = SpendoColor.TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                if (bank.isPrimary) {
                                    Text(
                                        text = "PRIMARY",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SpendoColor.AccentRed
                                    )
                                }
                            }
                        }

                        // Divider & Meta Readouts (Source, Last Updated, Recent Activity)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SpendoColor.Divider)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (bank.balanceSource) {
                                                BalanceSource.REPORTED -> SpendoColor.SuccessGreenSubtle
                                                BalanceSource.CALCULATED -> SpendoColor.SurfaceVariant
                                                BalanceSource.ESTIMATED -> SpendoColor.AccentRedSubtle
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = bank.balanceSource.name,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (bank.balanceSource) {
                                            BalanceSource.REPORTED -> SpendoColor.SuccessGreen
                                            BalanceSource.CALCULATED -> SpendoColor.TextSecondary
                                            BalanceSource.ESTIMATED -> SpendoColor.AccentRed
                                        }
                                    )
                                }

                                Text(
                                    text = bank.lastUpdatedText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = SpendoColor.TextMuted
                                )
                            }

                            bank.recentTransactionSummary?.let { summary ->
                                Text(
                                    text = summary,
                                    fontSize = 11.sp,
                                    color = SpendoColor.TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Add Account Button
            item {
                SpendoOutlinedButton(
                    text = "Add Bank Account",
                    onClick = { showAddBankDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Password Verification Dialog to Reveal Balances
    if (uiState.showUnlockDialog) {
        var passwordInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isVerifying by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                onToggleUnlockDialog(false)
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = SpendoColor.AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Reveal Bank Balances",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.TextPrimary,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your Spendo app password or PIN to unlock sensitive account balances for this session.",
                        fontSize = 13.sp,
                        color = SpendoColor.TextSecondary,
                        lineHeight = 18.sp
                    )
                    SpendoPasswordField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            errorMessage = null
                        },
                        label = "App Security Password / PIN",
                        placeholder = if (uiState.hasCustomPassword) "Enter password" else "Default PIN: 4892",
                        isError = errorMessage != null,
                        errorMessage = errorMessage
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (passwordInput.isBlank()) {
                            errorMessage = "Password cannot be empty"
                            return@TextButton
                        }
                        isVerifying = true
                        onVerifyAndUnlock(
                            passwordInput,
                            {
                                isVerifying = false
                                onToggleUnlockDialog(false)
                            },
                            {
                                isVerifying = false
                                errorMessage = "Incorrect password. Please try again."
                            }
                        )
                    },
                    enabled = !isVerifying
                ) {
                    Text(
                        text = if (isVerifying) "Verifying..." else "Unlock Balances",
                        color = SpendoColor.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onToggleUnlockDialog(false) },
                    enabled = !isVerifying
                ) {
                    Text("Cancel", color = SpendoColor.TextSecondary)
                }
            },
            containerColor = SpendoColor.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Add Bank Dialog
    if (showAddBankDialog) {
        var bankNameInput by remember { mutableStateOf("") }
        var accountTypeInput by remember { mutableStateOf("Savings") }
        var initialBalanceInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddBankDialog = false },
            title = {
                Text(
                    "Add Bank Account",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SpendoTextField(
                        value = bankNameInput,
                        onValueChange = { bankNameInput = it },
                        label = "Bank Name",
                        placeholder = "e.g. HDFC, SBI, ICICI, Axis"
                    )
                    SpendoTextField(
                        value = accountTypeInput,
                        onValueChange = { accountTypeInput = it },
                        label = "Account Type",
                        placeholder = "Savings / Current / Credit Card"
                    )
                    SpendoTextField(
                        value = initialBalanceInput,
                        onValueChange = { initialBalanceInput = it.filter { ch -> ch.isDigit() } },
                        label = "Initial Balance in ₹",
                        placeholder = "e.g. 25000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (bankNameInput.isNotBlank()) {
                            val paise = (initialBalanceInput.toLongOrNull() ?: 10000L) * 100
                            onAddBank(bankNameInput, accountTypeInput, paise)
                            showAddBankDialog = false
                        }
                    }
                ) {
                    Text("Add Account", color = SpendoColor.TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBankDialog = false }) {
                    Text("Cancel", color = SpendoColor.TextSecondary)
                }
            },
            containerColor = SpendoColor.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
