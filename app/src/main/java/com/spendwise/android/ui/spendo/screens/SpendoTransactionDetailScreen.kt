package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoBank
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.TransactionSource
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.theme.SpendoColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpendoTransactionDetailScreen(
    transactionId: String,
    transactions: List<SpendoTransaction>,
    onDeleteTransaction: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateTransaction: (SpendoTransaction) -> Unit = {},
    banks: List<SpendoBank> = emptyList()
) {
    val transaction = transactions.find { it.id == transactionId } ?: transactions.firstOrNull()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    // Edit form states
    var editAmountText by remember { mutableStateOf("") }
    var editMerchantText by remember { mutableStateOf("") }
    var editCategoryText by remember { mutableStateOf("") }
    var editBankName by remember { mutableStateOf("") }
    var editBankId by remember { mutableStateOf<String?>(null) }
    var editNoteText by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editValidationWarning by remember { mutableStateOf<String?>(null) }

    val openEditDialog = {
        if (transaction != null) {
            editAmountText = String.format(Locale.ROOT, "%.2f", transaction.amountRupees)
            editMerchantText = transaction.merchant
            editCategoryText = transaction.category
            editBankName = transaction.bankAccountName
            editBankId = transaction.bankAccountId
            editNoteText = transaction.note ?: ""
            editType = transaction.type
            editValidationWarning = null
            isEditing = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Transaction Details",
            categoryBadge = "LEDGER ENTRY",
            onNavigateBack = onNavigateBack
        )

        if (transaction == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Transaction record not found",
                    color = SpendoColor.TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Highlight Card
                SpendoDarkCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DotMatrixText(
                                text = if (transaction.type == TransactionType.EXPENSE) "DEBIT • POSTED" else "CREDIT • POSTED",
                                fontSize = 11.sp,
                                color = if (transaction.type == TransactionType.EXPENSE) SpendoColor.CardDarkTextSecondary else SpendoColor.SuccessGreen
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SpendoColor.SurfaceDark)
                                    .border(1.dp, SpendoColor.BorderDark, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = transaction.id,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SpendoColor.CardDarkTextSecondary
                                )
                            }
                        }

                        Text(
                            text = transaction.formattedAmount,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.CardDarkText
                        )

                        Text(
                            text = transaction.merchant,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = SpendoColor.CardDarkText
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
                        text = "Edit Transaction",
                        onClick = openEditDialog,
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

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Edit Transaction Modal Dialog
    if (isEditing && transaction != null) {
        val availableCategories = listOf(
            "Food & Drink", "Transit", "Groceries", "Shopping",
            "Bills & Utilities", "Entertainment", "Salary", "Freelance", "Other"
        )
        val availableBanks = if (banks.isNotEmpty()) {
            banks.map { it.bankName }
        } else {
            listOf("Indian Bank", "Kotak Mahindra Bank", "Karur Vysya Bank", "HDFC Bank", "Cash Wallet")
        }

        AlertDialog(
            onDismissRequest = { isEditing = false },
            title = {
                Text(
                    text = "Edit Transaction",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpendoColor.SurfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            TransactionType.EXPENSE to "Expense",
                            TransactionType.INCOME to "Income"
                        ).forEach { (type, label) ->
                            val isSelected = editType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SpendoColor.TextPrimary else androidx.compose.ui.graphics.Color.Transparent)
                                    .clickable { editType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) SpendoColor.Background else SpendoColor.TextSecondary
                                )
                            }
                        }
                    }

                    // Amount Input
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { editAmountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount (INR)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = SpendoColor.TextPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SpendoColor.Surface,
                            unfocusedContainerColor = SpendoColor.Surface,
                            focusedIndicatorColor = SpendoColor.TextPrimary,
                            unfocusedIndicatorColor = SpendoColor.Divider
                        )
                    )

                    // Merchant / Payee Input
                    OutlinedTextField(
                        value = editMerchantText,
                        onValueChange = { editMerchantText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Merchant / Description") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SpendoColor.Surface,
                            unfocusedContainerColor = SpendoColor.Surface,
                            focusedIndicatorColor = SpendoColor.TextPrimary,
                            unfocusedIndicatorColor = SpendoColor.Divider
                        )
                    )

                    // Category Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Category", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SpendoColor.TextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            availableCategories.forEach { cat ->
                                val isSelected = editCategoryText.equals(cat, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { editCategoryText = cat },
                                    label = { Text(cat, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SpendoColor.TextPrimary,
                                        selectedLabelColor = SpendoColor.Background,
                                        containerColor = SpendoColor.Surface,
                                        labelColor = SpendoColor.TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Bank Account Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Bank Account", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SpendoColor.TextSecondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            banks.forEach { bank ->
                                val isSelected = editBankId == bank.id || (editBankId == null && editBankName == bank.bankName)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        editBankId = bank.id
                                        editBankName = bank.displayName
                                    },
                                    label = { Text("${bank.displayName} (${bank.maskedAccountNumber})", fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SpendoColor.TextPrimary,
                                        selectedLabelColor = SpendoColor.Background,
                                        containerColor = SpendoColor.Surface,
                                        labelColor = SpendoColor.TextSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Optional Note
                    OutlinedTextField(
                        value = editNoteText,
                        onValueChange = { editNoteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note / Memo (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SpendoColor.Surface,
                            unfocusedContainerColor = SpendoColor.Surface,
                            focusedIndicatorColor = SpendoColor.TextPrimary,
                            unfocusedIndicatorColor = SpendoColor.Divider
                        )
                    )

                    // Error Warning
                    if (editValidationWarning != null) {
                        Text(
                            text = editValidationWarning ?: "",
                            color = SpendoColor.AccentRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanAmt = editAmountText.replace(",", "").trim()
                        val amtDouble = cleanAmt.toDoubleOrNull()
                        if (amtDouble == null || amtDouble <= 0.0) {
                            editValidationWarning = "Please enter a valid positive amount."
                            return@TextButton
                        }
                        if (editMerchantText.isBlank()) {
                            editValidationWarning = "Merchant description cannot be blank."
                            return@TextButton
                        }

                        val newPaise = Math.round(amtDouble * 100.0)
                        val updated = transaction.copy(
                            merchant = editMerchantText.trim(),
                            amountPaise = newPaise,
                            type = editType,
                            category = editCategoryText.ifBlank { "Other" },
                            bankAccountName = editBankName.ifBlank { "Primary Account" },
                            bankAccountId = editBankId ?: transaction.bankAccountId,
                            note = editNoteText.trim().ifBlank { null }
                        )

                        onUpdateTransaction(updated)
                        isEditing = false
                    }
                ) {
                    Text("Save Changes", color = SpendoColor.TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditing = false }) {
                    Text("Cancel", color = SpendoColor.TextSecondary)
                }
            },
            containerColor = SpendoColor.Surface,
            shape = RoundedCornerShape(20.dp)
        )
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
    badgeColor: androidx.compose.ui.graphics.Color? = null
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

        if (badgeColor != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeColor
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SpendoColor.TextPrimary
            )
        }
    }
}
