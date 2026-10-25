package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallReceived
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoIncomeScreen(
    uiState: SpendoUiState,
    onNavigateToAddIncome: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncomePaise = uiState.incomes.sumOf { it.amountPaise }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Income & Salary",
            categoryBadge = "INFLOW STREAMS",
            onNavigateBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Next Payday Countdown Card
            item {
                SpendoDarkCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DotGridBadge(
                                text = "Next Payday",
                                indicatorColor = SpendoColor.SuccessGreen,
                                containerColor = androidx.compose.ui.graphics.Color(0xFF262626),
                                textColor = SpendoColor.CardDarkText
                            )
                            val nextPayDateText = uiState.incomes.firstOrNull()?.nextPayDate ?: androidx.compose.runtime.remember {
                                val cal = java.util.Calendar.getInstance()
                                cal.add(java.util.Calendar.MONTH, 1)
                                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(cal.time)
                            }
                            Text(
                                text = nextPayDateText.uppercase(java.util.Locale.getDefault()),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.CardDarkText
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotMatrixText(text = "Total Monthly Inflow", fontSize = 11.sp, color = SpendoColor.CardDarkTextSecondary)
                            Text(
                                text = "₹%,.2f".format(totalIncomePaise / 100.0),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = SpendoColor.CardDarkText
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = SpendoColor.SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "7 days remaining until tech salary disbursement.",
                                fontSize = 12.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }
                    }
                }
            }

            // Recurring Income Streams Header
            item {
                DotMatrixText(text = "Recurring & Active Streams", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            // Incomes List
            items(uiState.incomes, key = { it.id }) { income ->
                SpendoCard(contentPadding = 18.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .background(SpendoColor.SuccessGreenSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.CallReceived,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFF388E3C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = income.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendoColor.TextPrimary
                                )
                                Text(
                                    text = "${income.frequency} • Next: ${income.nextPayDate}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SpendoColor.TextSecondary
                                )
                            }
                        }

                        Text(
                            text = income.formattedAmount,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Add Income Action
            item {
                SpendoOutlinedButton(
                    text = "Add Income Stream",
                    onClick = onNavigateToAddIncome,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
