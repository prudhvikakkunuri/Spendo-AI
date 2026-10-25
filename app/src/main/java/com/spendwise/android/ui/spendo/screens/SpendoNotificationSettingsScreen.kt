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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun SpendoNotificationSettingsScreen(
    uiState: SpendoUiState,
    onToggleDetection: (Boolean) -> Unit,
    onToggleApp: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = androidx.compose.runtime.remember {
        context.getSharedPreferences("spendo_reminder_prefs", android.content.Context.MODE_PRIVATE)
    }

    var morningEnabled by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(prefs.getBoolean(com.spendwise.android.notification.SpendoReminderScheduler.KEY_MORNING_ENABLED, true))
    }
    var eveningEnabled by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(prefs.getBoolean(com.spendwise.android.notification.SpendoReminderScheduler.KEY_EVENING_ENABLED, true))
    }
    var testNotificationSent by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Notifications & Reminders",
            categoryBadge = "AUTOMATION & SCHEDULES",
            onNavigateBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily Reminders Header
            item {
                DotMatrixText(text = "Daily Expense Reminders", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            // Morning Reminder Card
            item {
                SpendoDarkCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotGridBadge(
                                text = "9:00 AM Daily",
                                indicatorColor = if (morningEnabled) SpendoColor.SuccessGreen else SpendoColor.TextMuted,
                                containerColor = androidx.compose.ui.graphics.Color(0xFF262626),
                                textColor = SpendoColor.CardDarkText
                            )
                            Text(
                                text = "Morning Review Reminder 🌤️",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SpendoColor.CardDarkText
                            )
                            Text(
                                text = "Prompt to review yesterday's expenses and record any missing cash or UPI spending.",
                                fontSize = 12.sp,
                                color = SpendoColor.CardDarkTextSecondary
                            )
                        }

                        Switch(
                            checked = morningEnabled,
                            onCheckedChange = { isChecked ->
                                morningEnabled = isChecked
                                com.spendwise.android.notification.SpendoReminderScheduler.updatePreferences(
                                    context = context,
                                    morningEnabled = isChecked,
                                    morningHour = 9,
                                    morningMinute = 0,
                                    eveningEnabled = eveningEnabled,
                                    eveningHour = 20,
                                    eveningMinute = 30
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpendoColor.Surface,
                                checkedTrackColor = SpendoColor.SuccessGreen,
                                uncheckedThumbColor = SpendoColor.TextMuted,
                                uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFF333333)
                            )
                        )
                    }
                }
            }

            // Evening Reminder Card
            item {
                SpendoDarkCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotGridBadge(
                                text = "8:30 PM Daily",
                                indicatorColor = if (eveningEnabled) SpendoColor.SuccessGreen else SpendoColor.TextMuted,
                                containerColor = androidx.compose.ui.graphics.Color(0xFF262626),
                                textColor = SpendoColor.CardDarkText
                            )
                            Text(
                                text = "Evening Logging Reminder 🌙",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SpendoColor.CardDarkText
                            )
                            Text(
                                text = "Encourage logging today's outlays before you call it a day.",
                                fontSize = 12.sp,
                                color = SpendoColor.CardDarkTextSecondary
                            )
                        }

                        Switch(
                            checked = eveningEnabled,
                            onCheckedChange = { isChecked ->
                                eveningEnabled = isChecked
                                com.spendwise.android.notification.SpendoReminderScheduler.updatePreferences(
                                    context = context,
                                    morningEnabled = morningEnabled,
                                    morningHour = 9,
                                    morningMinute = 0,
                                    eveningEnabled = isChecked,
                                    eveningHour = 20,
                                    eveningMinute = 30
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpendoColor.Surface,
                                checkedTrackColor = SpendoColor.SuccessGreen,
                                uncheckedThumbColor = SpendoColor.TextMuted,
                                uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFF333333)
                            )
                        )
                    }
                }
            }

            // Test Reminder Action
            item {
                SpendoCard(contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = if (testNotificationSent) "Test Notification Dispatched!" else "Test Reminder Notification",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (testNotificationSent) SpendoColor.SuccessGreen else SpendoColor.TextPrimary
                            )
                            Text(
                                text = "Verify sound, banner, and quick-add actions immediately.",
                                fontSize = 12.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }

                        com.spendwise.android.ui.spendo.components.SpendoOutlinedButton(
                            text = "Test Now",
                            onClick = {
                                com.spendwise.android.notification.SpendoNotificationHelper.showTestNotification(context)
                                testNotificationSent = true
                            }
                        )
                    }
                }
            }

            // Detection Section Header
            item {
                DotMatrixText(text = "Automatic Notification Ingestion", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            // Master Switch Dark Card
            item {
                SpendoDarkCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotGridBadge(
                                text = if (uiState.isNotificationDetectionEnabled) "Active Ingestion" else "Paused",
                                indicatorColor = if (uiState.isNotificationDetectionEnabled) SpendoColor.SuccessGreen else SpendoColor.AccentRed,
                                containerColor = androidx.compose.ui.graphics.Color(0xFF262626),
                                textColor = SpendoColor.CardDarkText
                            )
                            Text(
                                text = "Auto-Detect Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SpendoColor.CardDarkText
                            )
                            Text(
                                text = "Listen for debit/credit alerts from permitted payment applications on device.",
                                fontSize = 12.sp,
                                color = SpendoColor.CardDarkTextSecondary
                            )
                        }

                        Switch(
                            checked = uiState.isNotificationDetectionEnabled,
                            onCheckedChange = onToggleDetection,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpendoColor.Surface,
                                checkedTrackColor = SpendoColor.SuccessGreen,
                                uncheckedThumbColor = SpendoColor.TextMuted,
                                uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFF333333)
                            )
                        )
                    }
                }
            }

            // Monitored Apps Allowlist
            item {
                DotMatrixText(text = "Monitored Banking & Payment Apps", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            items(uiState.monitoredApps) { (appName, isEnabled) ->
                SpendoCard(contentPadding = 14.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleMedium,
                            color = SpendoColor.TextPrimary
                        )

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { onToggleApp(appName) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpendoColor.Surface,
                                checkedTrackColor = SpendoColor.TextPrimary,
                                uncheckedThumbColor = SpendoColor.TextMuted,
                                uncheckedTrackColor = SpendoColor.SurfaceVariant
                            )
                        )
                    }
                }
            }

            // Privacy Reassurance
            item {
                SpendoCard(contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DotMatrixText(text = "Security Guarantee", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                        Text(
                            text = "Spendo filters notifications exclusively in local memory. OTPs, personal messages, and full bodies are never logged or stored.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = SpendoColor.TextSecondary
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
