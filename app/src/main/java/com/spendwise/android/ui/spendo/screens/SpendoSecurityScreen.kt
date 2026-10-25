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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoSecurityScreen(
    uiState: SpendoUiState,
    onToggleBiometric: (Boolean) -> Unit,
    onAutoLockChange: (String) -> Unit,
    onToggleBalanceConceal: (Boolean) -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAutoLockDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Security & App Lock",
            categoryBadge = "VAULT PROTECTION",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Status Card
            SpendoDarkCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DotMatrixText(text = "Security Level", fontSize = 11.sp, color = SpendoColor.CardDarkTextSecondary)
                    Text(
                        text = "Encrypted Local Protection",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.CardDarkText
                    )
                    Text(
                        text = "Biometric authentication and salted PBKDF2 password hashing protect your financial accounts and ledger on-device.",
                        fontSize = 12.sp,
                        color = SpendoColor.CardDarkTextSecondary
                    )
                }
            }

            // PIN / Password Management Card
            SpendoCard(contentPadding = 18.dp) {
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = SpendoColor.TextPrimary, modifier = Modifier.size(18.dp))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "App Security Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextPrimary
                            )
                            Text(
                                text = if (uiState.hasCustomPassword) "Active • Custom password configured" else "Default password (4892) in use",
                                fontSize = 12.sp,
                                color = if (uiState.hasCustomPassword) SpendoColor.SuccessGreen else SpendoColor.AccentRedForeground
                            )
                        }
                    }

                    SpendoOutlinedButton(
                        text = if (uiState.hasCustomPassword) "Change" else "Set",
                        onClick = onNavigateToChangePassword,
                        height = 36.dp
                    )
                }
            }

            // Biometric Toggle Card
            SpendoCard(contentPadding = 18.dp) {
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Fingerprint, contentDescription = null, tint = SpendoColor.TextPrimary, modifier = Modifier.size(18.dp))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Biometric Unlock",
                                style = MaterialTheme.typography.titleMedium,
                                color = SpendoColor.TextPrimary
                            )
                            Text(
                                text = "Use fingerprint or face recognition",
                                fontSize = 12.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = uiState.isBiometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SpendoColor.Surface,
                            checkedTrackColor = SpendoColor.TextPrimary,
                            uncheckedThumbColor = SpendoColor.TextMuted,
                            uncheckedTrackColor = SpendoColor.SurfaceVariant
                        )
                    )
                }
            }

            // Auto-Lock Preference
            SpendoCard(
                contentPadding = 18.dp,
                onClick = { showAutoLockDialog = true }
            ) {
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Timer, contentDescription = null, tint = SpendoColor.TextPrimary, modifier = Modifier.size(18.dp))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Auto-Lock Duration",
                                style = MaterialTheme.typography.titleMedium,
                                color = SpendoColor.TextPrimary
                            )
                            Text(
                                text = "Lock app when inactive: ${uiState.autoLockDuration}",
                                fontSize = 12.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "CHANGE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.AccentRed
                    )
                }
            }

            // Concealed Balances Default
            SpendoCard(contentPadding = 18.dp) {
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Outlined.Shield, contentDescription = null, tint = SpendoColor.TextPrimary, modifier = Modifier.size(18.dp))
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Conceal Balances by Default",
                                style = MaterialTheme.typography.titleMedium,
                                color = SpendoColor.TextPrimary
                            )
                            Text(
                                text = "Always show ₹•••••• until unlocked",
                                fontSize = 12.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = uiState.isBalanceConcealProtected,
                        onCheckedChange = onToggleBalanceConceal,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SpendoColor.Surface,
                            checkedTrackColor = SpendoColor.TextPrimary,
                            uncheckedThumbColor = SpendoColor.TextMuted,
                            uncheckedTrackColor = SpendoColor.SurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }


    if (showAutoLockDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = {
                Text("Select Auto-Lock Timing", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Immediately", "After 1 minute", "After 5 minutes").forEach { option ->
                        Text(
                            text = "• $option",
                            fontSize = 14.sp,
                            fontWeight = if (uiState.autoLockDuration == option) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAutoLockChange(option)
                                    showAutoLockDialog = false
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            containerColor = SpendoColor.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
