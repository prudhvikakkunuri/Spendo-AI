package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.TransactionSource
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoTransactionDetailScreen(
    transactionId: String,
    transactions: List<SpendoTransaction>,
    onDeleteTransaction: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transaction = transactions.find { it.id == transactionId } ?: transactions.firstOrNull()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Transaction Details",
            categoryBadge = "LEDGER ENTRY",
            onNavigateBack = onNavigateBack,
            actions = {
                IconButton(onClick = { showDeleteConfirmDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete",
                        tint = SpendoColor.AccentRed
                    )
                }
            }
        )

        if (transaction == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Transaction not found", color = SpendoColor.TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Amount Card
                SpendoDarkCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DotGridBadge(
                            text = if (transaction.type == TransactionType.INCOME) "Income Credited" else "Expense Debited",
                            indicatorColor = if (transaction.type == TransactionType.INCOME) SpendoColor.SuccessGreen else SpendoColor.AccentRed,
                            containerColor = Color(0xFF262626),
                            textColor = SpendoColor.TextInverse
                        )

                        Text(
                            text = "${if (transaction.type == TransactionType.INCOME) "+" else "−"} ${transaction.formattedAmount}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            color = SpendoColor.TextInverse
                        )

                        Text(
                            text = transaction.merchant,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = SpendoColor.TextInverse
                        )
                    }
                }

                // Metadata Details Card
                SpendoCard(contentPadding = 20.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DotMatrixText(text = "Metadata & Origin", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                        DetailRow(label = "Category", value = transaction.category)
                        DetailRow(label = "Bank / Account", value = transaction.bankAccountName)
                        DetailRow(label = "Timestamp", value = transaction.formattedDate)
                        DetailRow(
                            label = "Ingestion Source",
                            value = if (transaction.source == TransactionSource.NOTIFICATION) "Automatic Alert" else "Manual Entry",
                            badgeColor = if (transaction.source == TransactionSource.NOTIFICATION) SpendoColor.AccentRed else null
                        )
                        if (!transaction.note.isNullOrBlank()) {
                            DetailRow(label = "Note", value = transaction.note)
                        }
                    }
                }

                // Action Buttons: Edit and Delete
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpendoOutlinedButton(
                        text = "Edit Record (Mock)",
                        onClick = { /* Mock edit */ },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )

                    SpendoOutlinedButton(
                        text = "Delete Transaction",
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = SpendoColor.AccentRed.copy(alpha = 0.5f),
                        contentColor = SpendoColor.AccentRed,
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }

    if (showDeleteConfirmDialog && transaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("Delete Transaction?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This entry will be permanently removed from your Spendo ledger history.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(transaction.id)
                        showDeleteConfirmDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = SpendoColor.AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = SpendoColor.TextSecondary)
                }
            },
            containerColor = SpendoColor.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    badgeColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = SpendoColor.TextSecondary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (badgeColor != null) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
            }
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = SpendoColor.TextPrimary
            )
        }
    }
}
