package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoAnalyticsScreen(
    uiState: SpendoUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf("Weekly") }

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
                            .background(if (isSelected) SpendoColor.SurfaceDark else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { selectedTimeframe = timeframe }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeframe.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = if (isSelected) SpendoColor.TextInverse else SpendoColor.TextSecondary
                        )
                    }
                }
            }

            // Overview Metric Card
            SpendoDarkCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DotGridBadge(
                            text = "$selectedTimeframe Trend",
                            indicatorColor = SpendoColor.SuccessGreen,
                            containerColor = SpendoColor.DarkCardContainer,
                            textColor = SpendoColor.TextInverse
                        )
                        Text(
                            text = "−14% VS LAST PERIOD",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.SuccessGreen
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DotMatrixText(text = "Total Outflow", fontSize = 11.sp, color = SpendoColor.TextInverseMuted)
                        com.spendwise.android.ui.spendo.components.DotMatrixAmount(
                            amountText = "11,480.00",
                            fontSize = 32.sp,
                            color = SpendoColor.TextInverse
                        )
                    }

                    Text(
                        text = "Net savings rate is currently 42% of total inflows.",
                        fontSize = 12.sp,
                        color = SpendoColor.TextSecondary
                    )
                }
            }

            // Minimalist Nothing-style Weekly Bar Chart
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DotMatrixText(text = "Daily Inflow & Outflow Pattern", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    val barData = listOf(
                        "Mon" to 1200f,
                        "Tue" to 850f,
                        "Wed" to 2100f,
                        "Thu" to 1420f,
                        "Fri" to 2900f,
                        "Sat" to 1950f,
                        "Sun" to 1060f
                    )
                    val maxVal = 3200f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        barData.forEach { (day, amount) ->
                            val heightFraction = (amount / maxVal).coerceIn(0.1f, 1f)
                            val isPeak = amount >= 2500f

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.5f)
                                            .fillMaxHeight(heightFraction)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isPeak) SpendoColor.AccentRed else SpendoColor.SurfaceDark
                                            )
                                    )
                                }

                                Text(
                                    text = day,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendoColor.TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Highest Spending Day Highlight Card
            SpendoCard(contentPadding = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        DotMatrixText(text = "Peak Outflow Day", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                        Text(
                            text = "Friday (₹2,900.00)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.TextPrimary
                        )
                        Text(
                            text = "Dining and weekend groceries contributed 68%.",
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

            // Category Breakdown Strip
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DotMatrixText(text = "Category Share", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    val shares = listOf(
                        "Food & Dining" to 38,
                        "Transit & Cab" to 24,
                        "Groceries" to 19,
                        "Shopping" to 12,
                        "Subscriptions" to 7
                    )

                    shares.forEach { (cat, pct) ->
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
                                    text = "$pct%",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendoColor.TextSecondary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { pct / 100f },
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
