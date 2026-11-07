package com.spendwise.android.ui.spendo.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoEmptyState
import com.spendwise.android.ui.spendo.model.SpendoBank
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoTheme
import java.util.Locale

@Composable
fun SpendoReviewScreen(
    uiState: SpendoUiState,
    onNavigateBack: () -> Unit,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
    onUpdateAndApprove: (id: String, merchant: String, amountPaise: Long, category: String, bankAccountName: String, bankAccountId: String?, rememberPref: Boolean) -> Unit,
    onNavigateToBankAccounts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SpendoTheme.colors
    val pendingTransactions = uiState.pendingReviewTransactions
    val banks = uiState.banks

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                DotMatrixText(
                    text = "VERIFICATION DESK",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
                Text(
                    text = "Review Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            if (pendingTransactions.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accentRed.copy(alpha = 0.15f))
                        .border(1.dp, colors.accentRed, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${pendingTransactions.size} PENDING",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentRed
                    )
                }
            }
        }

        // Subtitle context banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = colors.accentRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Review-first protection: Spendo holds detected alerts in this queue until verified. Confirmed ledger and bank balances update only upon explicit approval.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (pendingTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                SpendoEmptyState(
                    title = "All Clear",
                    message = "No unverified notifications or pending transactions awaiting review. Your confirmed ledger is 100% clean."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingTransactions, key = { it.id }) { tx ->
                    ReviewTransactionItem(
                        transaction = tx,
                        banks = banks,
                        onApprove = { onApprove(tx.id) },
                        onReject = { onReject(tx.id) },
                        onUpdateAndApprove = { merchant, amountPaise, category, bank, bankId, rememberPref ->
                            onUpdateAndApprove(tx.id, merchant, amountPaise, category, bank, bankId, rememberPref)
                        },
                        onNavigateToBankAccounts = onNavigateToBankAccounts
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewTransactionItem(
    transaction: SpendoTransaction,
    banks: List<SpendoBank>,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onUpdateAndApprove: (merchant: String, amountPaise: Long, category: String, bank: String, bankId: String?, rememberPref: Boolean) -> Unit,
    onNavigateToBankAccounts: () -> Unit
) {
    val colors = SpendoTheme.colors
    var isExpanded by remember { mutableStateOf(false) }
    var editedMerchant by remember { mutableStateOf(transaction.merchant) }
    var editedCategory by remember { mutableStateOf(transaction.category) }
    var selectedBankId by remember { mutableStateOf(transaction.bankAccountId) }
    var selectedBankName by remember { mutableStateOf(transaction.bankAccountName) }
    var editedAmountPaise by remember { mutableStateOf(transaction.amountPaise) }
    var editedAmountStr by remember { mutableStateOf((transaction.amountPaise / 100.0).toString()) }
    var rememberPref by remember { mutableStateOf(true) }

    val hasLinkedAccount = !selectedBankId.isNullOrBlank()
    val isUnrecognizedSuffix = transaction.accountMatchStatus == com.spendwise.android.domain.banking.AccountMatchStatus.UNKNOWN_SUFFIX_UNLINKED ||
        (!transaction.extractedAccountSuffix.isNullOrBlank() && !hasLinkedAccount && banks.none { it.last4Digits == transaction.extractedAccountSuffix })

    SpendoCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Reason Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.accentRed.copy(alpha = 0.12f))
                        .border(1.dp, colors.accentRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = transaction.reviewReason ?: "PENDING VERIFICATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentRed
                    )
                }

                Text(
                    text = transaction.formattedDate,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }

            // Merchant & Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (hasLinkedAccount) {
                            "${transaction.category} • $selectedBankName"
                        } else {
                            "${transaction.category} • Unresolved Account"
                        },
                        fontSize = 12.sp,
                        color = if (hasLinkedAccount) colors.textSecondary else colors.accentRed
                    )
                }

                DotMatrixText(
                    text = "₹%.2f".format(Locale.ENGLISH, transaction.amountPaise / 100.0),
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
            }

            // If account is unlinked, ambiguous, or unrecognized
            if (!hasLinkedAccount) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentRed.copy(alpha = 0.08f))
                        .border(1.dp, colors.accentRed.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isUnrecognizedSuffix && !transaction.extractedAccountSuffix.isNullOrBlank()) {
                            Text(
                                text = "⚠️ Unrecognized bank account •••• ${transaction.extractedAccountSuffix}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentRed
                            )
                            Text(
                                text = "This account is not registered in Spendo. Link it to an existing account below, or add a new account.",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                lineHeight = 15.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Link to existing account:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.surface)
                                        .border(1.dp, colors.textPrimary, RoundedCornerShape(6.dp))
                                        .clickable { onNavigateToBankAccounts() }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "+ Add Bank Account",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "⚠️ Select the correct bank account",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentRed
                            )
                            if (!transaction.reviewReason.isNullOrBlank()) {
                                Text(
                                    text = transaction.reviewReason,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        if (banks.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(banks) { bank ->
                                    val isSelected = bank.id == selectedBankId
                                    val label = bank.displayNameWithSuffix
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) colors.textPrimary else colors.surface)
                                            .border(1.dp, if (isSelected) colors.textPrimary else colors.divider, RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedBankId = bank.id
                                                selectedBankName = label
                                            }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colors.background else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.textPrimary, RoundedCornerShape(6.dp))
                                    .clickable { onNavigateToBankAccounts() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "+ Add Bank Account to Link",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            if (!isExpanded) {
                // Quick Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (hasLinkedAccount) colors.surfaceDark else colors.surface)
                            .border(1.dp, if (hasLinkedAccount) colors.surfaceDark else colors.divider, RoundedCornerShape(8.dp))
                            .clickable {
                                if (hasLinkedAccount) {
                                    if (selectedBankId != transaction.bankAccountId || selectedBankName != transaction.bankAccountName) {
                                        onUpdateAndApprove(
                                            editedMerchant.trim(),
                                            editedAmountPaise,
                                            editedCategory.trim(),
                                            selectedBankName,
                                            selectedBankId,
                                            rememberPref
                                        )
                                    } else {
                                        onApprove()
                                    }
                                } else {
                                    // Expand to prompt account selection
                                    isExpanded = true
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasLinkedAccount) "✓ Approve" else "Select Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (hasLinkedAccount) (if (colors.isDark) colors.textPrimary else colors.textInverse) else colors.accentRed
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                            .clickable { isExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Edit",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                            .clickable { onReject() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Dismiss",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.accentRed
                        )
                    }
                }
            } else {
                // Expanded Edit Mode
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    OutlinedTextField(
                        value = editedMerchant,
                        onValueChange = { editedMerchant = it },
                        label = { Text("Merchant / Payee", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.background,
                            unfocusedContainerColor = colors.background,
                            focusedIndicatorColor = colors.textPrimary,
                            unfocusedIndicatorColor = colors.divider
                        )
                    )

                    OutlinedTextField(
                        value = editedAmountStr,
                        onValueChange = {
                            editedAmountStr = it
                            val parsed = it.toDoubleOrNull()
                            if (parsed != null && parsed > 0.0) {
                                editedAmountPaise = (parsed * 100).toLong()
                            }
                        },
                        label = { Text("Amount (₹)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.background,
                            unfocusedContainerColor = colors.background,
                            focusedIndicatorColor = colors.textPrimary,
                            unfocusedIndicatorColor = colors.divider
                        )
                    )

                    OutlinedTextField(
                        value = editedCategory,
                        onValueChange = { editedCategory = it },
                        label = { Text("Category (e.g. Food, Shopping, Transfer)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.background,
                            unfocusedContainerColor = colors.background,
                            focusedIndicatorColor = colors.textPrimary,
                            unfocusedIndicatorColor = colors.divider
                        )
                    )

                    // Bank Account Selector in Edit Mode
                    if (banks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Associated Bank Account",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(banks) { bank ->
                                    val isSelected = bank.id == selectedBankId
                                    val label = bank.displayNameWithSuffix
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) colors.textPrimary else colors.surface)
                                            .border(1.dp, if (isSelected) colors.textPrimary else colors.divider, RoundedCornerShape(6.dp))
                                            .clickable {
                                                selectedBankId = bank.id
                                                selectedBankName = label
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) colors.background else colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberPref = !rememberPref }
                    ) {
                        Checkbox(
                            checked = rememberPref,
                            onCheckedChange = { rememberPref = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.textPrimary,
                                checkmarkColor = colors.background
                            )
                        )
                        Text(
                            text = "Always remember this category for $editedMerchant",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (hasLinkedAccount) colors.surfaceDark else colors.surface)
                                .border(1.dp, if (hasLinkedAccount) colors.surfaceDark else colors.divider, RoundedCornerShape(8.dp))
                                .clickable {
                                    if (hasLinkedAccount) {
                                        onUpdateAndApprove(
                                            editedMerchant.trim(),
                                            editedAmountPaise,
                                            editedCategory.trim(),
                                            selectedBankName,
                                            selectedBankId,
                                            rememberPref
                                        )
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasLinkedAccount) "Save & Approve" else "Select Account First",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (hasLinkedAccount) (if (colors.isDark) colors.textPrimary else colors.textInverse) else colors.accentRed
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                                .clickable { isExpanded = false }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
