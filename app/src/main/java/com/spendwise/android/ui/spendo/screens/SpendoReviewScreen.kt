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
    onUpdateAndApprove: (id: String, merchant: String, amountPaise: Long, category: String, bankAccountName: String, rememberPref: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SpendoTheme.colors
    val pendingTransactions = uiState.pendingReviewTransactions

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
                    text = "Spendo flags ambiguous amounts, suspected transfers, and unconfirmed merchants so your tracked balance remains 100% reliable.",
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
                    message = "No uncertain notifications or ambiguous transactions awaiting review. Your tracked ledger is fully synchronized."
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
                        onApprove = { onApprove(tx.id) },
                        onReject = { onReject(tx.id) },
                        onUpdateAndApprove = { merchant, amountPaise, category, bank, rememberPref ->
                            onUpdateAndApprove(tx.id, merchant, amountPaise, category, bank, rememberPref)
                        }
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
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onUpdateAndApprove: (merchant: String, amountPaise: Long, category: String, bank: String, rememberPref: Boolean) -> Unit
) {
    val colors = SpendoTheme.colors
    var isExpanded by remember { mutableStateOf(false) }
    var editedMerchant by remember { mutableStateOf(transaction.merchant) }
    var editedCategory by remember { mutableStateOf(transaction.category) }
    var rememberPref by remember { mutableStateOf(true) }

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
                        text = transaction.reviewReason ?: "UNCERTAIN TRANSACTION",
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
                        text = "${transaction.category} • ${transaction.bankAccountName}",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                DotMatrixText(
                    text = "₹%.2f".format(Locale.ENGLISH, transaction.amountPaise / 100.0),
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
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
                            .background(colors.surfaceDark)
                            .clickable { onApprove() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✓ Approve",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (colors.isDark) colors.textPrimary else colors.textInverse
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
                        label = { Text("Merchant Name", fontSize = 11.sp) },
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
                                .background(colors.surfaceDark)
                                .clickable {
                                    onUpdateAndApprove(
                                        editedMerchant.trim(),
                                        transaction.amountPaise,
                                        editedCategory.trim(),
                                        transaction.bankAccountName,
                                        rememberPref
                                    )
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Save & Approve",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (colors.isDark) colors.textPrimary else colors.textInverse
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
