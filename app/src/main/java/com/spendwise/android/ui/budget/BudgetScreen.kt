package com.spendwise.android.ui.budget

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.domain.calculator.SafeToSpendResult
import com.spendwise.android.domain.model.Commitment
import com.spendwise.android.ui.common.SpendWiseTopBar
import com.spendwise.android.ui.theme.CharcoalBorder
import com.spendwise.android.ui.theme.CharcoalBorderSubtle
import com.spendwise.android.ui.theme.CharcoalDark
import com.spendwise.android.ui.theme.CreditGreen
import com.spendwise.android.ui.theme.DebitRed
import com.spendwise.android.ui.theme.LimeAccent
import com.spendwise.android.ui.theme.LimeAccentSubtle
import com.spendwise.android.ui.theme.ReviewOrange
import com.spendwise.android.ui.theme.SlateTextMuted
import com.spendwise.android.ui.theme.SlateTextSecondary
import com.spendwise.android.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel? = null
) {
    var previewIncome by remember { mutableStateOf("125000") }
    var previewBalance by remember { mutableStateOf("85000") }
    var previewReserve by remember { mutableStateOf("25000") }
    var previewSaved by remember { mutableStateOf(false) }

    val uiState = if (viewModel != null) {
        val state by viewModel.uiState.collectAsState()
        state
    } else {
        BudgetUiState(
            monthlyIncomeInput = previewIncome,
            startingBalanceInput = previewBalance,
            savingsReserveInput = previewReserve,
            commitments = listOf(
                Commitment(title = "House Rent", amountPaise = 2500000L, dueAtEpochMs = System.currentTimeMillis() + 864000000L, isPaid = false),
                Commitment(title = "Broadband & Utilities", amountPaise = 350000L, dueAtEpochMs = System.currentTimeMillis() + 364000000L, isPaid = true)
            ),
            safeToSpendResult = SafeToSpendResult(
                isComplete = true,
                incompleteReason = null,
                dailySafeToSpendPaise = 245000L,
                flexibleBudgetPaise = 2450000L,
                availableFundsPaise = 5200000L,
                baseFundsPaise = 8500000L,
                totalSpendThisMonthPaise = 3300000L,
                savingsReservePaise = 2500000L,
                remainingCommitmentsPaise = 2500000L,
                otherCreditsPaise = 0L,
                daysRemaining = 10,
                isStartingBalanceUsed = true
            ),
            isInitialized = true,
            saveMessage = if (previewSaved) "Budget settings saved! (Preview)" else null
        )
    }

    var showAddCommitmentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SpendWiseTopBar(
            title = "Budget & Safe-to-Spend",
            subtitle = "Daily spending guidance configuration",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Live Projected Daily Estimate Card
            val result = uiState.safeToSpendResult
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (result?.isComplete == true) LimeAccent.copy(alpha = 0.5f) else ReviewOrange.copy(alpha = 0.5f),
                        RoundedCornerShape(18.dp)
                    ),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROJECTED SAFE-TO-SPEND / DAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (result?.isComplete == true) LimeAccent else ReviewOrange,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = if (result?.isComplete == true) Icons.Default.Calculate else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (result?.isComplete == true) LimeAccent else ReviewOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (result?.isComplete == true) {
                        Text(
                            text = CurrencyFormatter.formatPaise(result.dailySafeToSpendPaise),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calculated across ${result.daysRemaining} remaining days in current month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    } else {
                        Text(
                            text = "Estimate Incomplete",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ReviewOrange
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = result?.incompleteReason ?: "Set a starting spendable balance or monthly income to calculate daily safe-to-spend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }
                }
            }

            // Notification / Banner if save message or error message exists
            uiState.saveMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CreditGreen.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CreditGreen)
                        Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = CreditGreen)
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DebitRed.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.WarningAmber, contentDescription = null, tint = DebitRed)
                        Text(text = error, style = MaterialTheme.typography.bodyMedium, color = DebitRed)
                    }
                }
            }

            // Budget Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "BUDGET BASELINE INPUTS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SlateTextMuted,
                        letterSpacing = 1.sp
                    )

                    BudgetInputField(
                        label = "Starting Spendable Balance (Optional ₹)",
                        value = uiState.startingBalanceInput,
                        onValueChange = {
                            if (viewModel != null) viewModel.onStartingBalanceChanged(it)
                            else previewBalance = it
                        },
                        hint = "Manually entered cash pool at period start (takes priority over income)"
                    )

                    BudgetInputField(
                        label = "Monthly Income Setting (Optional ₹)",
                        value = uiState.monthlyIncomeInput,
                        onValueChange = {
                            if (viewModel != null) viewModel.onMonthlyIncomeChanged(it)
                            else previewIncome = it
                        },
                        hint = "Expected monthly salary baseline (avoids double-counting with imported salary)"
                    )

                    BudgetInputField(
                        label = "Savings Reserve (₹)",
                        value = uiState.savingsReserveInput,
                        onValueChange = {
                            if (viewModel != null) viewModel.onSavingsReserveChanged(it)
                            else previewReserve = it
                        },
                        hint = "Protected emergency reserve ring-fenced from daily spending"
                    )
                }
            }

            // Upcoming Financial Commitments Card
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FINANCIAL COMMITMENTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SlateTextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Rent, EMIs, and upcoming bills",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )
                        }

                        OutlinedButton(
                            onClick = { showAddCommitmentDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (uiState.commitments.isEmpty()) {
                        Text(
                            text = "No upcoming commitments added. Tap 'Add' to track fixed monthly obligations.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
                            uiState.commitments.forEach { commitment ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel?.toggleCommitmentPaid(commitment) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (commitment.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = if (commitment.isPaid) "Paid" else "Unpaid",
                                                tint = if (commitment.isPaid) CreditGreen else SlateTextMuted
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = commitment.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (commitment.isPaid) SlateTextMuted else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Due: ${dateFormat.format(Date(commitment.dueAtEpochMs))} • ${if (commitment.isPaid) "Paid" else "Pending"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (commitment.isPaid) CreditGreen else SlateTextSecondary
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = CurrencyFormatter.formatPaise(commitment.amountPaise),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        IconButton(
                                            onClick = { viewModel?.deleteCommitment(commitment) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete commitment",
                                                tint = SlateTextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Spendable Breakdown & Calculated Totals
            if (result?.isComplete == true) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "MONTHLY SPENDING BREAKDOWN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextMuted,
                            letterSpacing = 1.sp
                        )

                        BreakdownRow(
                            label = if (result.isStartingBalanceUsed) "Starting Balance (Manual)" else "Base Monthly Income (Manual)",
                            value = CurrencyFormatter.formatPaise(result.baseFundsPaise),
                            isPositive = true
                        )

                        BreakdownRow(
                            label = "Confirmed Expenses (Ledger)",
                            value = "− ${CurrencyFormatter.formatPaise(result.totalSpendThisMonthPaise)}",
                            isPositive = false
                        )

                        if (result.otherCreditsPaise > 0L) {
                            BreakdownRow(
                                label = "Refunds & Reversals (Ledger)",
                                value = "+ ${CurrencyFormatter.formatPaise(result.otherCreditsPaise)}",
                                isPositive = true
                            )
                        }

                        BreakdownRow(
                            label = "Unpaid Commitments Due",
                            value = "− ${CurrencyFormatter.formatPaise(result.remainingCommitmentsPaise)}",
                            isPositive = false
                        )

                        BreakdownRow(
                            label = "Savings Reserve (Protected)",
                            value = "− ${CurrencyFormatter.formatPaise(result.savingsReservePaise)}",
                            isPositive = false
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CharcoalBorder)
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        BreakdownRow(
                            label = "Net Flexible Budget",
                            value = CurrencyFormatter.formatPaise(result.flexibleBudgetPaise),
                            isBold = true,
                            isPositive = true
                        )
                    }
                }
            }

            // Formula and Transparency Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = LimeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Formula & Transparency",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "1. Available Funds = Base Funds (Starting Balance or Monthly Income) + Non-Income Credits − Confirmed Expenses\n" +
                               "2. Flexible Budget = max(0, Available Funds − Unpaid Commitments − Savings Reserve)\n" +
                               "3. Safe-to-Spend / Day = Flexible Budget ÷ max(1, Remaining Days)",
                        style = MaterialTheme.typography.bodySmall,
                        color = LimeAccent,
                        lineHeight = 20.sp
                    )

                    Text(
                        text = SafeToSpendResult.DISCLAIMER,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (viewModel != null) viewModel.saveSettings()
                    else previewSaved = true
                },
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
                        text = "Save Budget Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Commitment Dialog
    if (showAddCommitmentDialog) {
        var commitmentTitle by remember { mutableStateOf("") }
        var commitmentAmount by remember { mutableStateOf("") }
        var commitmentDueDay by remember { mutableIntStateOf(1) }

        AlertDialog(
            onDismissRequest = { showAddCommitmentDialog = false },
            title = { Text("Add Financial Commitment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = commitmentTitle,
                        onValueChange = { commitmentTitle = it },
                        label = { Text("Title (e.g. Rent, EMI, Wifi)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = commitmentAmount,
                        onValueChange = { commitmentAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount in ₹") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = commitmentDueDay.toString(),
                        onValueChange = {
                            val parsed = it.toIntOrNull()
                            if (parsed != null && parsed in 1..31) commitmentDueDay = parsed
                        },
                        label = { Text("Due Day of Month (1 - 31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.addCommitment(commitmentTitle, commitmentAmount, commitmentDueDay)
                        showAddCommitmentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = CharcoalDark)
                ) {
                    Text("Add Commitment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCommitmentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BudgetInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            prefix = {
                Text(
                    text = "₹ ",
                    color = LimeAccent,
                    fontWeight = FontWeight.Bold
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = LimeAccent,
                unfocusedIndicatorColor = CharcoalBorderSubtle
            )
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = SlateTextMuted,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    isPositive: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else SlateTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                isBold -> LimeAccent
                isPositive -> MaterialTheme.colorScheme.onSurface
                else -> DebitRed
            }
        )
    }
}
