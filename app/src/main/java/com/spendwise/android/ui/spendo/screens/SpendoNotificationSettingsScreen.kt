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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Notification Detection",
            categoryBadge = "AUTOMATION",
            onNavigateBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
