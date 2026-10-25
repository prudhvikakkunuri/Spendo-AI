package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixAmount
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.AffordabilityVerdict
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.theme.SpendoColor
import java.util.Locale

@Composable
fun SpendoAnalyticsScreen(
    uiState: SpendoUiState,
    onNavigateBack: () -> Unit,
    onEvaluateAffordability: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf("Monthly") }
    var purchaseInputText by remember { mutableStateOf("") }

    // Dynamic Financial Calculations from local ledger
    val expenseTransactions = uiState.transactions.filter { it.type == TransactionType.EXPENSE }
    val totalExpensePaise = expenseTransactions.sumOf { it.amountPaise }
    val monthSpentRupees = totalExpensePaise / 100.0

    // Dynamic Category Spend Breakdown
    val categoryTotals = expenseTransactions
        .groupBy { it.category }
        .mapValues { (_, txs) -> txs.sumOf { it.amountPaise } }
        .toList()
        .sortedByDescending { it.second }

    // Top Merchant
    val topMerchantEntry = expenseTransactions
        .groupBy { it.merchant }
        .mapValues { (_, txs) -> txs.sumOf { it.amountPaise } }
        .maxByOrNull { it.value }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Spending Analytics",
            categoryBadge = "INSIGHTS & VELOCITY",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeframe Segmented Switcher (Daily, Weekly, Monthly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SpendoColor.Surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Daily", "Weekly", "Monthly").forEach { timeframe ->
                    val isSelected = selectedTimeframe == timeframe
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) SpendoColor.TextPrimary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { selectedTimeframe = timeframe }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeframe.uppercase(Locale.ROOT),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = if (isSelected) SpendoColor.Background else SpendoColor.TextSecondary
                        )
                    }
                }
            }

            // Overview Metric Card (Dynamically computed)
            SpendoDarkCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DotGridBadge(
                            text = "$selectedTimeframe Velocity",
                            indicatorColor = SpendoColor.SuccessGreen,
                            containerColor = SpendoColor.DarkCardContainer,
                            textColor = SpendoColor.CardDarkText
                        )
                        DotMatrixText(
                            text = "DETERMINISTIC DATA",
                            fontSize = 10.sp,
                            color = SpendoColor.CardDarkTextSecondary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DotMatrixText(text = "Total Tracked Outflow", fontSize = 11.sp, color = SpendoColor.CardDarkTextSecondary)
                        DotMatrixAmount(
                            amountText = "%,.2f".format(Locale.ENGLISH, monthSpentRupees),
                            fontSize = 32.sp,
                            color = SpendoColor.CardDarkText
                        )
                        DotMatrixText(
                            text = "Locally calculated from ${expenseTransactions.size} verified expenses",
                            fontSize = 10.sp,
                            color = SpendoColor.CardDarkTextSecondary
                        )
                    }
                }
            }

            // Highest Spending Merchant Highlight
            topMerchantEntry?.let { (merchant, amountPaise) ->
                SpendoCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            DotMatrixText(text = "Top Expense Destination", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                            Text(
                                text = "$merchant (₹%,.2f)".format(Locale.ENGLISH, amountPaise / 100.0),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextPrimary
                            )
                            val pct = if (totalExpensePaise > 0) (amountPaise.toDouble() / totalExpensePaise) * 100 else 0.0
                            Text(
                                text = "Accounts for %.1f%% of all tracked expenses.".format(Locale.ENGLISH, pct),
                                fontSize = 12.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.AccentRed)
                        )
                    }
                }
            }

            // Dynamic Category Breakdown
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DotMatrixText(text = "Category Distribution", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    if (categoryTotals.isEmpty()) {
                        Text(
                            text = "No category data available yet.",
                            fontSize = 12.sp,
                            color = SpendoColor.TextSecondary
                        )
                    } else {
                        categoryTotals.take(6).forEach { (cat, amountPaise) ->
                            val pct = if (totalExpensePaise > 0) ((amountPaise.toDouble() / totalExpensePaise) * 100).toInt() else 0
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SpendoColor.TextPrimary
                                    )
                                    Text(
                                        text = "₹%,.2f ($pct%)".format(Locale.ENGLISH, amountPaise / 100.0),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SpendoColor.TextSecondary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp)),
                                    color = SpendoColor.SurfaceDark,
                                    trackColor = SpendoColor.SurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Savings Goals with Progress Tracking
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DotMatrixText(text = "Savings Goals & Reserves", fontSize = 11.sp, color = SpendoColor.TextSecondary)
                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                            tint = SpendoColor.TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    uiState.savingsGoals.forEach { goal ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpendoColor.SurfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = goal.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SpendoColor.TextPrimary
                                )
                                Text(
                                    text = goal.percentageText,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SpendoColor.SuccessGreen
                                )
                            }

                            LinearProgressIndicator(
                                progress = { goal.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = SpendoColor.SuccessGreen,
                                trackColor = SpendoColor.DarkTrack
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Saved: ₹%,.0f / ₹%,.0f".format(Locale.ENGLISH, goal.savedPaise / 100.0, goal.targetPaise / 100.0),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SpendoColor.TextSecondary
                                )
                                Text(
                                    text = "Target: ${goal.targetDateText}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SpendoColor.TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Purchase Affordability Calculator
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DotMatrixText(text = "Purchase Affordability Calculator", fontSize = 11.sp, color = SpendoColor.TextSecondary)
                    Text(
                        text = "Evaluates whether an upcoming discretionary purchase is safe based on your liquid balances, active commitments, and savings goals.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = SpendoColor.TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = purchaseInputText,
                            onValueChange = { purchaseInputText = it },
                            placeholder = { Text("e.g. 4500", fontSize = 13.sp) },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SpendoColor.Background,
                                unfocusedContainerColor = SpendoColor.Background,
                                focusedIndicatorColor = SpendoColor.TextPrimary,
                                unfocusedIndicatorColor = SpendoColor.Divider
                            )
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpendoColor.TextPrimary)
                                .clickable {
                                    val amountPaise = (purchaseInputText.toDoubleOrNull()?.times(100))?.toLong() ?: 0L
                                    if (amountPaise > 0L) {
                                        onEvaluateAffordability(amountPaise)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Evaluate",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = SpendoColor.Background
                            )
                        }
                    }

                    // Result Display
                    uiState.affordabilityResult?.let { eval ->
                        val (verdictColor, verdictIcon) = when (eval.verdict) {
                            AffordabilityVerdict.AFFORDABLE -> SpendoColor.SuccessGreen to Icons.Outlined.CheckCircle
                            AffordabilityVerdict.CAUTION -> SpendoColor.AccentRed.copy(alpha = 0.8f) to Icons.Outlined.Info
                            AffordabilityVerdict.RISKY -> SpendoColor.AccentRed to Icons.Outlined.Warning
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpendoColor.Background)
                                .border(1.dp, verdictColor, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = verdictIcon,
                                            contentDescription = null,
                                            tint = verdictColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = eval.verdict.name,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = verdictColor
                                        )
                                    }

                                    DotMatrixText(
                                        text = "Item: ₹%,.2f".format(Locale.ENGLISH, eval.itemAmountPaise / 100.0),
                                        fontSize = 12.sp,
                                        color = SpendoColor.TextPrimary
                                    )
                                }

                                Text(
                                    text = eval.reasonText,
                                    fontSize = 12.sp,
                                    color = SpendoColor.TextPrimary,
                                    lineHeight = 16.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Liquid: ₹%,.0f".format(Locale.ENGLISH, eval.liquidBalancePaise / 100.0),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = SpendoColor.TextSecondary
                                    )
                                    Text(
                                        text = "Bills: ₹%,.0f".format(Locale.ENGLISH, eval.upcomingBillsPaise / 100.0),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = SpendoColor.TextSecondary
                                    )
                                    Text(
                                        text = "Free Buffer: ₹%,.0f".format(Locale.ENGLISH, eval.freeDiscretionaryPaise / 100.0),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = SpendoColor.SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
