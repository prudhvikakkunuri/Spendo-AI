package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoEmptyState
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoCalendarScreen(
    uiState: SpendoUiState,
    onSelectDay: (Int) -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var monthOffset by remember { mutableStateOf(0) }
    val displayMonth = when (monthOffset) {
        -1 -> "September 2026"
        1 -> "November 2026"
        else -> "October 2026"
    }

    // Days with transactions mock map
    val daysWithSpend = setOf(3, 7, 12, 16, 18, 22, 24)
    val todayDate = 24

    val dayTransactions = uiState.transactions.filter {
        // Mock matching: for day 24 return today's items, day 22 return past items
        when (uiState.selectedCalendarDay) {
            24 -> it.id in listOf("tx_1", "tx_2", "tx_3")
            22 -> it.id in listOf("tx_4", "tx_5")
            else -> it.id == "tx_6"
        }
    }

    val selectedDayTotalPaise = dayTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amountPaise }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                DotMatrixText(text = "Timeline", fontSize = 11.sp, color = SpendoColor.TextSecondary)
                Text(
                    text = "Calendar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary
                )
            }
        }

        // Calendar Card
        item {
            SpendoCard(contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Month Selector Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (monthOffset > -1) monthOffset-- },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = SpendoColor.TextPrimary
                            )
                        }

                        Text(
                            text = displayMonth.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = SpendoColor.TextPrimary
                        )

                        IconButton(
                            onClick = { if (monthOffset < 1) monthOffset++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = SpendoColor.TextPrimary
                            )
                        }
                    }

                    // Weekday Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Calendar Days Grid (5 weeks of October 2026)
                    val daysInMonth = 31
                    val startingEmptyDays = 3 // Thu 1st Oct
                    val totalCells = 35

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (week in 0 until 5) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 7) {
                                    val cellIndex = week * 7 + col
                                    val dayNum = cellIndex - startingEmptyDays + 1

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNum in 1..daysInMonth) {
                                            val isSelected = uiState.selectedCalendarDay == dayNum
                                            val isToday = dayNum == todayDate
                                            val hasSpend = daysWithSpend.contains(dayNum)

                                            Column(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isSelected -> SpendoColor.TextPrimary
                                                            isToday -> SpendoColor.SurfaceVariant
                                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                                        }
                                                    )
                                                    .clickable { onSelectDay(dayNum) },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) SpendoColor.Background else SpendoColor.TextPrimary
                                                )
                                                if (hasSpend && !isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(3.dp)
                                                            .clip(CircleShape)
                                                            .background(SpendoColor.AccentRed)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Spend Summary Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    DotMatrixText(
                        text = "${uiState.selectedCalendarDay} OCTOBER 2026",
                        fontSize = 11.sp,
                        color = SpendoColor.TextSecondary
                    )
                    Text(
                        text = "Daily Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.TextPrimary
                    )
                }

                Text(
                    text = "Total: ₹%,.2f".format(selectedDayTotalPaise / 100.0),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SpendoColor.TextPrimary
                )
            }
        }

        // Selected Day Transactions
        if (dayTransactions.isEmpty()) {
            item {
                SpendoEmptyState(
                    title = "No Expenses Logged",
                    message = "No financial activity was recorded on this day."
                )
            }
        } else {
            items(dayTransactions, key = { it.id }) { tx ->
                HomeTransactionRow(
                    transaction = tx,
                    onClick = { onNavigateToTransactionDetail(tx.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
