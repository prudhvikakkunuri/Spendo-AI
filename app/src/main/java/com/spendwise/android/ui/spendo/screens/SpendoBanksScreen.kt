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
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoBank
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoBanksScreen(
    uiState: SpendoUiState,
    onToggleUnlockDialog: (Boolean) -> Unit,
    onUnlockBalances: () -> Unit,
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
                        imageVector = if (uiState.isBalancesRevealed) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                        contentDescription = "Toggle Balance Privacy",
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
                                textColor = SpendoColor.TextInverse
                            )
                            Text(
                                text = if (uiState.isBalancesRevealed) "UNLOCKED" else "CONCEALED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isBalancesRevealed) SpendoColor.SuccessGreen else SpendoColor.AccentRed
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotMatrixText(text = "Aggregated Balance", fontSize = 11.sp, color = SpendoColor.TextInverseMuted)
                            if (uiState.isBalancesRevealed) {
                                DotMatrixAmount(
                                    amountText = "%,.2f".format(totalBalancePaise / 100.0),
                                    fontSize = 32.sp,
                                    color = SpendoColor.TextInverse
                                )
                            } else {
                                Text(
                                    text = "₹ • • • • • •",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    letterSpacing = 2.sp,
                                    color = SpendoColor.TextInverse
                                )
                            }
                        }
                    }
                }
            }

            // Bank Accounts List
            items(uiState.banks, key = { it.id }) { bank ->
                SpendoCard(contentPadding = 18.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SpendoColor.SurfaceDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AccountBalance,
                                    contentDescription = null,
                                    tint = SpendoColor.TextInverse,
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
                            Text(
                                text = if (uiState.isBalancesRevealed) bank.formattedBalance else "₹••••••",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = SpendoColor.TextPrimary
                            )
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

    // Mock Unlock Balances Dialog (Biometric / PIN prompt)
    if (uiState.showUnlockDialog) {
        var pinInput by remember { mutableStateOf("") }
        var isPinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { onToggleUnlockDialog(false) },
            title = {
                Text("Unlock Balances", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter your 4-digit Spendo security PIN or confirm biometrics to reveal concealed balances.",
                        fontSize = 13.sp,
                        color = SpendoColor.TextSecondary
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4) {
                                pinInput = it
                                isPinError = false
                            }
                        },
                        label = { Text("4-digit PIN") },
                        placeholder = { Text("Default: 4892") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = isPinError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isPinError) {
                        Text("Incorrect PIN. Try 4892.", fontSize = 11.sp, color = SpendoColor.AccentRed)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinInput == "4892" || pinInput == uiState.appPin || pinInput.length == 4) {
                            onUnlockBalances()
                        } else {
                            isPinError = true
                        }
                    }
                ) {
                    Text("Unlock", color = SpendoColor.TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onToggleUnlockDialog(false) }) {
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
                Text("Add Bank Account", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bankNameInput,
                        onValueChange = { bankNameInput = it },
                        label = { Text("Bank Name (e.g. Axis, Kotak)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = accountTypeInput,
                        onValueChange = { accountTypeInput = it },
                        label = { Text("Account Type") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = initialBalanceInput,
                        onValueChange = { initialBalanceInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Initial Balance in ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
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
                    Text("Add", color = SpendoColor.TextPrimary, fontWeight = FontWeight.Bold)
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
