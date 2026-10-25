package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoBank
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoAddTransactionScreen(
    banks: List<SpendoBank>,
    initialType: TransactionType = TransactionType.EXPENSE,
    onSaveTransaction: (merchant: String, amountPaise: Long, type: TransactionType, category: String, bankName: String, note: String?, bankAccountId: String?) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var amountInput by remember { mutableStateOf("") }
    var transactionType by remember { mutableStateOf(initialType) }
    var merchantInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Drink") }
    var selectedBankId by remember { mutableStateOf(banks.firstOrNull()?.id) }
    var selectedBankName by remember { mutableStateOf(banks.firstOrNull()?.displayName ?: "HDFC Bank") }

    val expenseCategories = listOf("Food & Drink", "Transit", "Groceries", "Shopping", "Bills & Utilities", "Entertainment", "Other")
    val incomeCategories = listOf("Salary", "Freelance", "Dividends", "Gift / Refund", "Other Income")

    val activeCategories = if (transactionType == TransactionType.EXPENSE) expenseCategories else incomeCategories

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = if (transactionType == TransactionType.EXPENSE) "Record Expense" else "Record Income",
            categoryBadge = "MANUAL ENTRY",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Direction Selector: Expense vs Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SpendoColor.Surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income"
                ).forEach { (type, label) ->
                    val isSelected = transactionType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) SpendoColor.TextPrimary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable {
                                transactionType = type
                                selectedCategory = if (type == TransactionType.EXPENSE) expenseCategories.first() else incomeCategories.first()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = if (isSelected) SpendoColor.Background else SpendoColor.TextSecondary
                        )
                    }
                }
            }

            // Oversized Amount Input Card
            SpendoCard(contentPadding = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DotMatrixText(
                        text = "Amount (INR)",
                        fontSize = 11.sp,
                        color = SpendoColor.TextSecondary
                    )

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        prefix = {
                            Text(
                                text = "₹ ",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = SpendoColor.TextPrimary
                            )
                        },
                        placeholder = {
                            Text(
                                text = "0.00",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 32.sp,
                                color = SpendoColor.TextMuted
                            )
                        },
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.TextPrimary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SpendoColor.Surface,
                            unfocusedContainerColor = SpendoColor.Surface,
                            focusedIndicatorColor = SpendoColor.TextPrimary,
                            unfocusedIndicatorColor = SpendoColor.Divider
                        )
                    )
                }
            }

            // Merchant / Payee Details Card
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DotMatrixText(text = "Transaction Details", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    OutlinedTextField(
                        value = merchantInput,
                        onValueChange = { merchantInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Merchant or Payee *") },
                        placeholder = { Text("e.g. Starbucks, Uber, Rent") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SpendoColor.Surface,
                            unfocusedContainerColor = SpendoColor.Surface,
                            focusedIndicatorColor = SpendoColor.TextPrimary,
                            unfocusedIndicatorColor = SpendoColor.Divider
                        )
                    )

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Optional Note") },
                        placeholder = { Text("Context or memo") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SpendoColor.Surface,
                            unfocusedContainerColor = SpendoColor.Surface,
                            focusedIndicatorColor = SpendoColor.TextPrimary,
                            unfocusedIndicatorColor = SpendoColor.Divider
                        )
                    )
                }
            }

            // Category Selection
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DotMatrixText(text = "Category", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeCategories.forEach { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = { Text(category, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isSelected) SpendoColor.TextPrimary else SpendoColor.Divider),
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
            }

            // Bank Account Selector
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DotMatrixText(text = "Payment Source", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        banks.forEach { bank ->
                            val isSelected = selectedBankId == bank.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedBankId = bank.id
                                    selectedBankName = bank.displayName
                                },
                                label = { Text("${bank.displayName} (${bank.maskedAccountNumber})", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isSelected) SpendoColor.TextPrimary else SpendoColor.Divider),
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
            }

            // Action Buttons: Save & Cancel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SpendoPrimaryButton(
                    text = "Save Entry",
                    onClick = {
                        val parsedAmount = (amountInput.toDoubleOrNull() ?: 0.0)
                        val paise = (parsedAmount * 100).toLong()
                        if (paise > 0L) {
                            onSaveTransaction(
                                merchantInput,
                                paise,
                                transactionType,
                                selectedCategory,
                                selectedBankName,
                                noteInput.ifBlank { null },
                                selectedBankId
                            )
                            onNavigateBack()
                        }
                    },
                    enabled = amountInput.toDoubleOrNull() != null && (amountInput.toDoubleOrNull() ?: 0.0) > 0.0
                )

                SpendoOutlinedButton(
                    text = "Cancel",
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
