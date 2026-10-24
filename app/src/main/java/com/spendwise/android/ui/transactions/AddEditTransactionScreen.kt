package com.spendwise.android.ui.transactions

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.ui.common.ErrorStateBanner
import com.spendwise.android.ui.common.SpendWiseTopBar
import com.spendwise.android.ui.theme.CharcoalBorder
import com.spendwise.android.ui.theme.CharcoalDark
import com.spendwise.android.ui.theme.CreditGreen
import com.spendwise.android.ui.theme.DebitRed
import com.spendwise.android.ui.theme.LimeAccent
import com.spendwise.android.ui.theme.SlateTextMuted

@Composable
fun AddEditTransactionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditTransactionViewModel? = null
) {
    val uiState = if (viewModel != null) {
        val state by viewModel.uiState.collectAsState()
        state
    } else {
        AddEditTransactionUiState(
            amountInput = "450",
            merchantInput = "Coffee Shop",
            noteInput = "Espresso with team",
            category = TransactionCategory.FOOD,
            direction = TransactionDirection.DEBIT
        )
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SpendWiseTopBar(
            title = if (uiState.isEditMode) "Edit Transaction" else "Add Expense",
            subtitle = if (uiState.isEditMode) "Update existing entry" else "Manual transaction entry",
            onNavigateBack = onNavigateBack,
            actions = {
                if (uiState.isEditMode) {
                    IconButton(onClick = { viewModel?.deleteTransaction() }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete transaction",
                            tint = DebitRed
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.errorMessage != null) {
                ErrorStateBanner(errorMessage = uiState.errorMessage!!)
            }

            // Direction Selector Card (Debit vs Credit)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRANSACTION TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DirectionChoiceButton(
                            title = "Debit (Expense)",
                            isSelected = uiState.direction == TransactionDirection.DEBIT,
                            direction = TransactionDirection.DEBIT,
                            onClick = { viewModel?.onDirectionChange(TransactionDirection.DEBIT) },
                            modifier = Modifier.weight(1f)
                        )
                        DirectionChoiceButton(
                            title = "Credit (Income)",
                            isSelected = uiState.direction == TransactionDirection.CREDIT,
                            direction = TransactionDirection.CREDIT,
                            onClick = { viewModel?.onDirectionChange(TransactionDirection.CREDIT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Amount Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "AMOUNT (INR)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.amountInput,
                        onValueChange = { viewModel?.onAmountChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = LimeAccent,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        placeholder = { Text("0.00", color = SlateTextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = LimeAccent,
                            unfocusedIndicatorColor = CharcoalBorder
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Stored as integer paise without floating-point inaccuracy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextMuted
                    )
                }
            }

            // Merchant / Description
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "DETAILS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextMuted
                    )

                    OutlinedTextField(
                        value = uiState.merchantInput,
                        onValueChange = { viewModel?.onMerchantChange(it) },
                        label = { Text("Merchant or Description *") },
                        placeholder = { Text("e.g. Swiggy, Metro, Rent") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = LimeAccent,
                            unfocusedIndicatorColor = CharcoalBorder
                        )
                    )

                    OutlinedTextField(
                        value = uiState.noteInput,
                        onValueChange = { viewModel?.onNoteChange(it) },
                        label = { Text("Optional Note") },
                        placeholder = { Text("Add context, invoice no, or items") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = LimeAccent,
                            unfocusedIndicatorColor = CharcoalBorder
                        )
                    )
                }
            }

            // Category Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TransactionCategory.values().forEach { category ->
                            FilterChip(
                                selected = uiState.category == category,
                                onClick = { viewModel?.onCategoryChange(category) },
                                label = { Text(category.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LimeAccent,
                                    selectedLabelColor = CharcoalDark
                                )
                            )
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = { viewModel?.saveTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimeAccent,
                    contentColor = CharcoalDark
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Text(
                        text = if (uiState.isEditMode) "Update Transaction" else "Save Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DirectionChoiceButton(
    title: String,
    isSelected: Boolean,
    direction: TransactionDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        if (direction == TransactionDirection.DEBIT) DebitRed else CreditGreen
    } else CharcoalBorder

    val containerColor = if (isSelected) {
        if (direction == TransactionDirection.DEBIT) DebitRed.copy(alpha = 0.15f) else CreditGreen.copy(alpha = 0.15f)
    } else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (direction == TransactionDirection.DEBIT) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (direction == TransactionDirection.DEBIT) DebitRed else CreditGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
