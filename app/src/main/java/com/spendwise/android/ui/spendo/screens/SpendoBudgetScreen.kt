package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import com.spendwise.android.ui.spendo.components.DotMatrixAmount
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoTextField
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoBudgetScreen(
    uiState: SpendoUiState,
    onUpdateLimits: (dailyPaise: Long, monthlyPaise: Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyLimitRupees = uiState.dailyLimitPaise / 100.0
    val todaySpentRupees = uiState.todaySpentPaise / 100.0
    val dailyRemaining = (dailyLimitRupees - todaySpentRupees).coerceAtLeast(0.0)
    val dailyProgress = if (dailyLimitRupees > 0.0) (todaySpentRupees / dailyLimitRupees).coerceIn(0.0, 1.0).toFloat() else 0f

    val monthlyLimitRupees = uiState.monthlyLimitPaise / 100.0
    val monthSpentRupees = uiState.monthSpentPaise / 100.0
    val monthlyRemaining = (monthlyLimitRupees - monthSpentRupees).coerceAtLeast(0.0)
    val monthlyProgress = if (monthlyLimitRupees > 0.0) (monthSpentRupees / monthlyLimitRupees).coerceIn(0.0, 1.0).toFloat() else 0f

    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Budget Management",
            categoryBadge = "LIMITS & CEILINGS",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily Budget Card
            SpendoDarkCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DotMatrixText(text = "Daily Allowance", fontSize = 11.sp, color = SpendoColor.CardDarkTextSecondary)
                        Text(
                            text = "${(dailyProgress * 100).toInt()}% USED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SpendoColor.CardDarkText
                        )
                    }

                    DotMatrixAmount(
                        amountText = "%,.2f".format(todaySpentRupees),
                        fontSize = 32.sp,
                        color = SpendoColor.CardDarkText
                    )

                    LinearProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (dailyProgress > 0.85f) SpendoColor.AccentRed else SpendoColor.CardDarkText,
                        trackColor = SpendoColor.DarkTrack
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ceiling: ₹%,.0f".format(dailyLimitRupees),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SpendoColor.CardDarkTextSecondary
                        )
                        Text(
                            text = "Remaining: ₹%,.0f".format(dailyRemaining),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.CardDarkText
                        )
                    }
                }
            }

            // Monthly Budget Card
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DotMatrixText(text = "Monthly Spending Pool", fontSize = 11.sp, color = SpendoColor.TextSecondary)
                        Text(
                            text = "${(monthlyProgress * 100).toInt()}% USED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = SpendoColor.TextPrimary
                        )
                    }

                    DotMatrixAmount(
                        amountText = "%,.2f".format(monthSpentRupees),
                        fontSize = 28.sp,
                        color = SpendoColor.TextPrimary
                    )

                    LinearProgressIndicator(
                        progress = { monthlyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = SpendoColor.TextPrimary,
                        trackColor = SpendoColor.SurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Pool: ₹%,.0f".format(monthlyLimitRupees),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = SpendoColor.TextSecondary
                        )
                        Text(
                            text = "Remaining: ₹%,.0f".format(monthlyRemaining),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.TextPrimary
                        )
                    }
                }
            }

            // Category Spending Breakdown
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DotMatrixText(text = "Category Expenses", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    val expenseTransactions = uiState.transactions.filter { it.type == com.spendwise.android.ui.spendo.model.TransactionType.EXPENSE }
                    val categorySpending = expenseTransactions
                        .groupBy { it.category }
                        .mapValues { (_, txs) -> txs.sumOf { it.amountPaise } / 100.0 }
                        .toList()
                        .sortedByDescending { it.second }

                    val totalSpent = categorySpending.sumOf { it.second }

                    if (categorySpending.isEmpty()) {
                        Text(
                            text = "No category expenses recorded yet. Actual expenses will appear here automatically.",
                            fontSize = 12.sp,
                            color = SpendoColor.TextSecondary
                        )
                    } else {
                        categorySpending.forEach { (category, current) ->
                            val fraction = if (totalSpent > 0.0) (current / totalSpent).coerceIn(0.0, 1.0).toFloat() else 0f
                            val pct = (fraction * 100).toInt()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SpendoColor.TextPrimary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "₹%,.2f (%d%%)".format(current, pct),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = SpendoColor.TextSecondary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { fraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp)),
                                    color = SpendoColor.TextPrimary,
                                    trackColor = SpendoColor.SurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Edit Action Button
            SpendoPrimaryButton(
                text = "Edit Budget Ceilings",
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(84.dp))
        }
    }

    if (showEditDialog) {
        var dailyInput by remember { mutableStateOf((dailyLimitRupees).toInt().toString()) }
        var monthlyInput by remember { mutableStateOf((monthlyLimitRupees).toInt().toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("Edit Budget Ceilings", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = SpendoColor.TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpendoTextField(
                        value = dailyInput,
                        onValueChange = { dailyInput = it.filter { ch -> ch.isDigit() } },
                        label = "Daily Limit in ₹",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    SpendoTextField(
                        value = monthlyInput,
                        onValueChange = { monthlyInput = it.filter { ch -> ch.isDigit() } },
                        label = "Monthly Limit in ₹",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val d = (dailyInput.toLongOrNull() ?: 2500L) * 100
                        val m = (monthlyInput.toLongOrNull() ?: 65000L) * 100
                        onUpdateLimits(d, m)
                        showEditDialog = false
                    }
                ) {
                    Text("Save", color = SpendoColor.TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = SpendoColor.TextSecondary)
                }
            },
            containerColor = SpendoColor.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
