package com.spendwise.android.ui.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spendwise.android.SpendWiseApplication
import com.spendwise.android.domain.calculator.SafeToSpendResult
import com.spendwise.android.domain.model.CategorySpendSummary
import com.spendwise.android.domain.model.SourceType
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.domain.model.TransactionCategory
import com.spendwise.android.domain.model.TransactionDirection
import com.spendwise.android.notification.NotificationAccessManager
import com.spendwise.android.notification.NotificationAccessState
import com.spendwise.android.ui.common.EmptyStateView
import com.spendwise.android.ui.theme.CharcoalBorder
import com.spendwise.android.ui.theme.CharcoalBorderSubtle
import com.spendwise.android.ui.theme.CharcoalDark
import com.spendwise.android.ui.theme.CreditGreen
import com.spendwise.android.ui.theme.CreditGreenSubtle
import com.spendwise.android.ui.theme.DebitRed
import com.spendwise.android.ui.theme.DebitRedSubtle
import com.spendwise.android.ui.theme.LimeAccent
import com.spendwise.android.ui.theme.LimeAccentSubtle
import com.spendwise.android.ui.theme.ReviewOrange
import com.spendwise.android.ui.theme.ReviewOrangeSubtle
import com.spendwise.android.ui.theme.SlateTextMuted
import com.spendwise.android.ui.theme.SlateTextSecondary
import com.spendwise.android.util.CurrencyFormatter

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel? = null,
    accessManager: NotificationAccessManager? = null,
    onNavigateToBudget: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToNotificationSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState = if (viewModel != null) {
        val state by viewModel.uiState.collectAsState()
        state
    } else {
        DashboardUiState(
            spentTodayPaise = 150000L,
            receivedTodayPaise = 0L,
            monthSpendPaise = 1550000L,
            safeToSpendResult = SafeToSpendResult(
                isComplete = true,
                incompleteReason = null,
                dailySafeToSpendPaise = 245000L,
                flexibleBudgetPaise = 2450000L,
                availableFundsPaise = 5200000L,
                baseFundsPaise = 8500000L,
                totalSpendThisMonthPaise = 1550000L,
                savingsReservePaise = 2500000L,
                remainingCommitmentsPaise = 2500000L,
                otherCreditsPaise = 0L,
                daysRemaining = 10,
                isStartingBalanceUsed = true
            ),
            categorySpendSummaries = listOf(
                CategorySpendSummary(TransactionCategory.FOOD, 650000L, 8, 42),
                CategorySpendSummary(TransactionCategory.SHOPPING, 520000L, 3, 33),
                CategorySpendSummary(TransactionCategory.TRANSPORT, 380000L, 5, 25)
            ),
            pendingReviewCount = 0,
            recentTransactions = emptyList(),
            isLoading = false
        )
    }

    val context = LocalContext.current
    val effectiveAccessManager = accessManager ?: (context.applicationContext as? SpendWiseApplication)?.notificationAccessManager
    var detectionStatus by remember {
        mutableStateOf(effectiveAccessManager?.getOverallStatus() ?: NotificationAccessState.ACTIVE)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                detectionStatus = effectiveAccessManager?.getOverallStatus() ?: NotificationAccessState.ACTIVE
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = LimeAccent,
                contentColor = CharcoalDark,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add manual expense"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Header with Branded Emblem & Live Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LimeAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "SpendWise Logo",
                            tint = CharcoalDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SpendWise",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Local-First Expense Intelligence",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }
                }

                val (badgeText, badgeColor) = when (detectionStatus) {
                    NotificationAccessState.ACTIVE -> Pair("Active", CreditGreen)
                    NotificationAccessState.PAUSED -> Pair("Paused", ReviewOrange)
                    NotificationAccessState.PERMISSION_NEEDED -> Pair("Enable", ReviewOrange)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable { onNavigateToNotificationSetup() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(badgeColor)
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Safe-to-Spend Hero Card
            val isComplete = uiState.isSafeToSpendComplete
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isComplete) LimeAccent.copy(alpha = 0.4f) else ReviewOrange.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onNavigateToBudget() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
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
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = if (isComplete) LimeAccent else ReviewOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "DAILY SAFE-TO-SPEND",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isComplete) LimeAccent else ReviewOrange,
                                letterSpacing = 1.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Configure budget",
                            tint = SlateTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isComplete) {
                        Text(
                            text = CurrencyFormatter.formatPaise(uiState.safeToSpendEstimatePaise),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Daily spendable allowance for ${uiState.safeToSpendResult?.daysRemaining ?: 1} days remaining in this month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3-Metric Summary Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Flexible Pool",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateTextSecondary
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaise(uiState.safeToSpendResult?.flexibleBudgetPaise ?: 0L),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "Days Left",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateTextSecondary
                                )
                                Text(
                                    text = "${uiState.safeToSpendResult?.daysRemaining ?: 0} days",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(
                                    text = "Month Spend",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateTextSecondary
                                )
                                Text(
                                    text = CurrencyFormatter.formatPaise(uiState.monthSpendPaise),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Estimate Incomplete",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ReviewOrange
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Set your monthly income or starting balance to calculate daily spending limits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = ReviewOrange,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Tap here to configure budget settings →",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ReviewOrange
                            )
                        }
                    }
                }
            }

            // Daily Stats (Live from Room)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Spent Today",
                    value = CurrencyFormatter.formatPaise(uiState.spentTodayPaise),
                    direction = TransactionDirection.DEBIT,
                    onClick = onNavigateToTransactions,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Received Today",
                    value = CurrencyFormatter.formatPaise(uiState.receivedTodayPaise),
                    direction = TransactionDirection.CREDIT,
                    onClick = onNavigateToTransactions,
                    modifier = Modifier.weight(1f)
                )
            }

            // Monthly Spending Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
                    .clickable { onNavigateToTransactions() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = LimeAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Month-to-Date Spend",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatPaise(uiState.monthSpendPaise),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View ledger",
                        tint = SlateTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Category Spending Breakdown Section
            if (uiState.categorySpendSummaries.isNotEmpty()) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = LimeAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Category Spending (This Month)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            uiState.categorySpendSummaries.forEach { categorySummary ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = categorySummary.category.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${categorySummary.percentageOfTotal}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SlateTextSecondary
                                            )
                                            Text(
                                                text = CurrencyFormatter.formatPaise(categorySummary.totalSpendPaise),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { categorySummary.percentageOfTotal / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = LimeAccent,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Review Queue Banner (if items exist)
            if (uiState.pendingReviewCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ReviewOrange.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable { onNavigateToReview() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ReviewOrange.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = ReviewOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${uiState.pendingReviewCount} Transactions Need Review",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Uncertain notification alerts awaiting confirmation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = ReviewOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Recent Transactions Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LimeAccent,
                    modifier = Modifier.clickable { onNavigateToTransactions() }
                )
            }

            // Transaction Items or Empty State
            if (uiState.recentTransactions.isEmpty()) {
                EmptyStateView(
                    title = "No Expenses Logged Yet",
                    message = "Tap the + button to record an expense or turn on auto-detection from your payment apps in Settings."
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    uiState.recentTransactions.forEach { transaction ->
                        RecentTransactionItem(
                            transaction = transaction,
                            onClick = onNavigateToTransactions
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    direction: TransactionDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, CharcoalBorderSubtle, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (direction == TransactionDirection.DEBIT) DebitRedSubtle else CreditGreenSubtle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (direction == TransactionDirection.DEBIT) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (direction == TransactionDirection.DEBIT) "Debit" else "Credit",
                        tint = if (direction == TransactionDirection.DEBIT) DebitRed else CreditGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RecentTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorderSubtle, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.direction == TransactionDirection.DEBIT)
                                DebitRedSubtle
                            else
                                CreditGreenSubtle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.direction == TransactionDirection.DEBIT)
                            Icons.Default.ArrowUpward
                        else
                            Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (transaction.direction == TransactionDirection.DEBIT) DebitRed else CreditGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = transaction.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                        if (transaction.sourceType == SourceType.NOTIFICATION) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LimeAccentSubtle)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Alert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LimeAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "${if (transaction.direction == TransactionDirection.DEBIT) "−" else "+"} ${CurrencyFormatter.formatPaise(transaction.amountPaise)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (transaction.direction == TransactionDirection.DEBIT)
                    MaterialTheme.colorScheme.onSurface
                else
                    CreditGreen
            )
        }
    }
}
