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
import androidx.compose.runtime.mutableIntStateOf
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SpendoCalendarScreen(
    uiState: SpendoUiState,
    onSelectDay: (Int) -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceNow = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(deviceNow.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(deviceNow.get(Calendar.MONTH)) } // 0-based
    var selectedDay by remember { mutableIntStateOf(deviceNow.get(Calendar.DAY_OF_MONTH)) }

    val monthCal = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply {
            clear()
            set(selectedYear, selectedMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val startOfMonthEpochMs = monthCal.timeInMillis
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Ensure selectedDay does not exceed maximum days in month
    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

    val nextMonthCal = remember(selectedYear, selectedMonth) {
        Calendar.getInstance().apply {
            clear()
            if (selectedMonth == 11) {
                set(selectedYear + 1, 0, 1, 0, 0, 0)
            } else {
                set(selectedYear, selectedMonth + 1, 1, 0, 0, 0)
            }
            set(Calendar.MILLISECOND, 0)
        }
    }
    val endOfMonthEpochMs = nextMonthCal.timeInMillis

    // First day of week (Monday = 0, Sunday = 6)
    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK)
    val startingEmptyDays = when (firstDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val displayMonth = monthYearFormat.format(monthCal.time).uppercase(Locale.getDefault())

    val navigatePrevMonth = {
        if (selectedMonth == 0) {
            selectedMonth = 11
            selectedYear -= 1
        } else {
            selectedMonth -= 1
        }
        selectedDay = 1
        onSelectDay(1)
    }

    val navigateNextMonth = {
        if (selectedMonth == 11) {
            selectedMonth = 0
            selectedYear += 1
        } else {
            selectedMonth += 1
        }
        selectedDay = 1
        onSelectDay(1)
    }

    // Filter transactions occurring strictly within this month
    val monthTransactions = remember(uiState.transactions, startOfMonthEpochMs, endOfMonthEpochMs) {
        uiState.transactions.filter {
            it.occurredAtEpochMs in startOfMonthEpochMs until endOfMonthEpochMs
        }
    }

    // Days with activity within this month
    val daysWithSpend = remember(monthTransactions, selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        val set = mutableSetOf<Int>()
        for (tx in monthTransactions) {
            cal.timeInMillis = tx.occurredAtEpochMs
            if (cal.get(Calendar.YEAR) == selectedYear && cal.get(Calendar.MONTH) == selectedMonth) {
                set.add(cal.get(Calendar.DAY_OF_MONTH))
            }
        }
        set
    }

    // Selected day transactions
    val dayTransactions = remember(monthTransactions, selectedDay, selectedYear, selectedMonth) {
        val cal = Calendar.getInstance()
        monthTransactions.filter {
            cal.timeInMillis = it.occurredAtEpochMs
            cal.get(Calendar.YEAR) == selectedYear &&
            cal.get(Calendar.MONTH) == selectedMonth &&
            cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    val selectedDayTotalPaise = dayTransactions
        .filter { it.type == TransactionType.EXPENSE }
        .sumOf { it.amountPaise }

    val totalCells = startingEmptyDays + daysInMonth
    val totalWeeks = (totalCells + 6) / 7

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
                            onClick = navigatePrevMonth,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = SpendoColor.TextPrimary
                            )
                        }

                        Text(
                            text = displayMonth,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = SpendoColor.TextPrimary
                        )

                        IconButton(
                            onClick = navigateNextMonth,
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

                    // Dynamic Calendar Days Grid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (week in 0 until totalWeeks) {
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
                                            val isSelected = selectedDay == dayNum
                                            val isRealToday = (selectedYear == deviceNow.get(Calendar.YEAR) &&
                                                    selectedMonth == deviceNow.get(Calendar.MONTH) &&
                                                    dayNum == deviceNow.get(Calendar.DAY_OF_MONTH))
                                            val hasSpend = daysWithSpend.contains(dayNum)

                                            Column(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isSelected -> SpendoColor.TextPrimary
                                                            isRealToday -> SpendoColor.SurfaceVariant
                                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                                        }
                                                    )
                                                    .clickable {
                                                        selectedDay = dayNum
                                                        onSelectDay(dayNum)
                                                    },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isRealToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) SpendoColor.Background else SpendoColor.TextPrimary
                                                )
                                                if (hasSpend && !isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
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
                        text = "$selectedDay $displayMonth",
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
                    text = "Total: ₹%,.2f".format(Locale.ENGLISH, selectedDayTotalPaise / 100.0),
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
                    message = "No financial activity was recorded on $selectedDay ${monthYearFormat.format(monthCal.time)}."
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
