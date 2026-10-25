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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.spendwise.android.ui.spendo.components.DotMatrixPattern
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.theme.SpendoColor

import com.spendwise.android.ui.spendo.theme.ThemeMode

@Composable
fun SpendoSettingsScreen(
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onNavigateToSecurity: () -> Unit,
    onNavigateToBanks: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToStreaks: () -> Unit,
    onNavigateToAiConfig: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToImportExport: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Settings",
            categoryBadge = "SYSTEM & PREFERENCES",
            onNavigateBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Identity Hero Card
            item {
                SpendoDarkCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotMatrixText(text = "Spendo Mobile", fontSize = 11.sp, color = SpendoColor.CardDarkTextSecondary)
                            Text(
                                text = "Your Money. Your Agent.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.CardDarkText
                            )
                            Text(
                                text = "Version 1.0.0 • Nothing Edition",
                                fontSize = 12.sp,
                                color = SpendoColor.CardDarkTextSecondary
                            )
                        }

                        DotMatrixPattern(color = androidx.compose.ui.graphics.Color(0xFF333333))
                    }
                }
            }

            // Section: Appearance & Theme
            item {
                DotMatrixText(text = "Appearance & Interface", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                SpendoCard(contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Theme Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SpendoColor.TextPrimary
                                )
                                Text(
                                    text = "Consistent monochrome styling across all screens",
                                    fontSize = 12.sp,
                                    color = SpendoColor.TextSecondary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(SpendoColor.SurfaceVariant)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to "System",
                                ThemeMode.LIGHT to "Light",
                                ThemeMode.DARK to "Dark"
                            ).forEach { (mode, label) ->
                                val isSelected = currentThemeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(if (isSelected) SpendoColor.TextPrimary else androidx.compose.ui.graphics.Color.Transparent)
                                        .clickable { onThemeModeChange(mode) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SpendoColor.Background else SpendoColor.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Security & Access
            item {
                DotMatrixText(text = "Security & Privacy", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                SpendoCard(contentPadding = 8.dp) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Outlined.Lock,
                            title = "App Lock & PIN Security",
                            subtitle = "PIN code, biometrics, and auto-lock",
                            onClick = onNavigateToSecurity
                        )
                        SettingsNavRow(
                            icon = Icons.Outlined.AccountBalance,
                            title = "Banks & Concealed Balances",
                            subtitle = "Vault privacy and account management",
                            onClick = onNavigateToBanks
                        )
                    }
                }
            }

            // Section: Intelligence & Automation
            item {
                DotMatrixText(text = "Intelligence & Ingestion", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                SpendoCard(contentPadding = 8.dp) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Outlined.SmartToy,
                            title = "AI Provider Configuration",
                            subtitle = "Groq, Cerebras, Gemini, OpenAI credentials",
                            onClick = onNavigateToAiConfig
                        )
                        SettingsNavRow(
                            icon = Icons.Outlined.Notifications,
                            title = "Notification Detection Settings",
                            subtitle = "Payment apps listener and auto-categorization",
                            onClick = onNavigateToNotifications
                        )
                    }
                }
            }

            // Section: Financial Plans
            item {
                DotMatrixText(text = "Plans & Ledger", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                SpendoCard(contentPadding = 8.dp) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Outlined.Category,
                            title = "Budgets & Spending Limits",
                            subtitle = "Daily and monthly expenditure ceilings",
                            onClick = onNavigateToBudgets
                        )
                        SettingsNavRow(
                            icon = Icons.Outlined.Payments,
                            title = "Income & Salary Schedule",
                            subtitle = "Salary timeline and recurring streams",
                            onClick = onNavigateToIncome
                        )
                        SettingsNavRow(
                            icon = Icons.Outlined.EmojiEvents,
                            title = "Streaks & Milestones",
                            subtitle = "Discipline tracking and achievements",
                            onClick = onNavigateToStreaks
                        )
                        SettingsNavRow(
                            icon = Icons.Outlined.CloudUpload,
                            title = "Data Import & Export",
                            subtitle = "Download CSV or backup archives",
                            onClick = onNavigateToImportExport
                        )
                    }
                }
            }

            // Section: About & Info
            item {
                SpendoCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Info, contentDescription = null, tint = SpendoColor.TextSecondary)
                        Column {
                            Text(
                                text = "On-Device Processing Guarantee",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextPrimary
                            )
                            Text(
                                text = "All financial records remain local to your Android device. Spendo operates with zero third-party telemetry.",
                                fontSize = 12.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
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
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SpendoColor.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SpendoColor.TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = SpendoColor.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpendoColor.TextSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = SpendoColor.TextMuted,
            modifier = Modifier.size(14.dp)
        )
    }
}
