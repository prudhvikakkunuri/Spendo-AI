package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixAmount
import com.spendwise.android.ui.spendo.components.DotMatrixPattern
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoEmptyState
import com.spendwise.android.ui.spendo.components.SpendoMascotWidget
import com.spendwise.android.ui.spendo.model.SpendoTransaction
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.model.TransactionType
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoHomeScreen(
    uiState: SpendoUiState,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToBanks: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    onNavigateToActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyLimitRupees = uiState.dailyLimitPaise / 100.0
    val todaySpentRupees = uiState.todaySpentPaise / 100.0
    val remainingRupees = (dailyLimitRupees - todaySpentRupees).coerceAtLeast(0.0)
    val progressFraction = (todaySpentRupees / dailyLimitRupees).coerceIn(0.0, 1.0).toFloat()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpendoColor.Background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = SpendoColor.SurfaceDark,
                contentColor = SpendoColor.TextInverse,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Header: Greeting, Status, Settings Shortcut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SpendoColor.AccentRed)
                            )
                            DotMatrixText(
                                text = "Spendo Agent 1.0",
                                fontSize = 10.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }
                        Text(
                            text = "Hello, Alex",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = SpendoColor.TextPrimary
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = SpendoColor.TextPrimary
                            )
                        }
                    }
                }
            }

            // Hero Daily Safe-to-Spend Dark Card
            item {
                SpendoDarkCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DotGridBadge(
                                text = "${uiState.currentStreakDays} Day Streak",
                                indicatorColor = SpendoColor.SuccessGreen,
                                containerColor = SpendoColor.DarkCardContainer,
                                textColor = SpendoColor.TextInverse
                            )
                            DotMatrixPattern(color = SpendoColor.DotPatternDark)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotMatrixText(
                                text = "Today's Expenses",
                                fontSize = 11.sp,
                                color = SpendoColor.TextInverseMuted
                            )
                            DotMatrixAmount(
                                amountText = "%,.2f".format(todaySpentRupees),
                                fontSize = 36.sp,
                                color = SpendoColor.TextInverse,
                                showRedDot = true
                            )
                        }

                        // Minimal Budget Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (progressFraction > 0.85f) SpendoColor.AccentRed else SpendoColor.SuccessGreen,
                                trackColor = SpendoColor.DarkTrack
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Limit: ₹%,.0f".format(dailyLimitRupees),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SpendoColor.TextInverseMuted
                                )
                                Text(
                                    text = "Remaining: ₹%,.0f".format(remainingRupees),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendoColor.TextInverse
                                )
                            }
                        }
                    }
                }
            }

            // Spendo AI Mascot Widget
            item {
                SpendoMascotWidget(
                    message = "You have used ${(progressFraction * 100).toInt()}% of your daily safe limit. Financial velocity is disciplined.",
                    statusText = "AI AGENT • MONITORING"
                )
            }

            // Quick Actions Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HomeQuickActionButton(
                        title = "Expense",
                        icon = Icons.Outlined.ArrowOutward,
                        onClick = onNavigateToAddTransaction,
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickActionButton(
                        title = "Income",
                        icon = Icons.Outlined.CallReceived,
                        onClick = onNavigateToAddIncome,
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickActionButton(
                        title = "Banks",
                        icon = Icons.Outlined.AccountBalance,
                        onClick = onNavigateToBanks,
                        modifier = Modifier.weight(1f)
                    )
                    HomeQuickActionButton(
                        title = "Analytics",
                        icon = Icons.Outlined.Insights,
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DotMatrixText(
                        text = "Recent Transactions",
                        fontSize = 11.sp,
                        color = SpendoColor.TextSecondary
                    )
                    Text(
                        text = "VIEW ALL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SpendoColor.AccentRed,
                        modifier = Modifier.clickable { onNavigateToActivity() }
                    )
                }
            }

            // Transactions List or Empty State
            if (uiState.transactions.isEmpty()) {
                item {
                    SpendoEmptyState(
                        title = "No Transactions Recorded",
                        message = "Your ledger is currently empty. Tap + below to add your first expense or link automatic alerts.",
                        actionLabel = "Add Expense",
                        onActionClick = onNavigateToAddTransaction
                    )
                }
            } else {
                items(uiState.transactions.take(5), key = { it.id }) { tx ->
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
}

@Composable
private fun HomeQuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SpendoCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = 12.dp,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SpendoColor.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = SpendoColor.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = SpendoColor.TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun HomeTransactionRow(
    transaction: SpendoTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SpendoCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = 14.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.type == TransactionType.INCOME)
                                SpendoColor.SuccessGreenSubtle
                            else
                                SpendoColor.SurfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.type == TransactionType.INCOME)
                            Icons.Outlined.CallReceived
                        else
                            Icons.Outlined.ArrowOutward,
                        contentDescription = null,
                        tint = SpendoColor.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = transaction.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SpendoColor.TextPrimary
                    )
                    Text(
                        text = "${transaction.category} • ${transaction.bankAccountName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SpendoColor.TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val sign = if (transaction.type == TransactionType.INCOME) "+" else "−"
                Text(
                    text = "$sign${transaction.formattedAmount}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SpendoColor.TextPrimary
                )
                Text(
                    text = transaction.formattedDate,
                    fontSize = 11.sp,
                    color = SpendoColor.TextMuted
                )
            }
        }
    }
}
