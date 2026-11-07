package com.spendwise.android.ui.spendo.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.spendwise.android.notification.SpendoNotificationHelper
import com.spendwise.android.notification.SpendoReminderScheduler
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpendoNotificationSettingsScreen(
    uiState: SpendoUiState,
    onToggleDetection: (Boolean) -> Unit,
    onToggleApp: (packageName: String, isEnabled: Boolean) -> Unit,
    onRefreshPermission: () -> Unit = {},
    onToneChange: (com.spendwise.android.notification.NotificationPersonalityTone) -> Unit = {},
    onTestParse: (String) -> Unit = {},
    onClearTestResult: () -> Unit = {},
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Auto-refresh permission status whenever user returns from Android System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val prefs = remember {
        context.getSharedPreferences("spendo_reminder_prefs", Context.MODE_PRIVATE)
    }

    var morningEnabled by remember {
        mutableStateOf(prefs.getBoolean(SpendoReminderScheduler.KEY_MORNING_ENABLED, true))
    }
    var eveningEnabled by remember {
        mutableStateOf(prefs.getBoolean(SpendoReminderScheduler.KEY_EVENING_ENABLED, true))
    }
    var testNotificationSent by remember { mutableStateOf(false) }

    // Diagnostic parser test input
    var testInputText by remember {
        mutableStateOf("Your a/c XXXXXXXXXXXX9362 is debited Rs. 50.00 on 24-Oct-2026 to BHAVAPRITA STATIONERY AND EMBROIDERY WORK info :P2A/291530634673. Avl Bal INR 59.2 Not You? call 18005721916-KVB")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Notifications & Monitor",
            categoryBadge = "AUTOMATION & ACCESS",
            onNavigateBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Android Permission Status Banner
            item {
                DotMatrixText(text = "Android System Access", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                if (uiState.isNotificationAccessGranted) {
                    SpendoDarkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DotGridBadge(
                                    text = "Access Authorized",
                                    indicatorColor = SpendoColor.SuccessGreen,
                                    containerColor = androidx.compose.ui.graphics.Color(0xFF262626),
                                    textColor = SpendoColor.CardDarkText
                                )
                                Text(
                                    text = "Notification Listener Active",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SpendoColor.CardDarkText
                                )
                                Text(
                                    text = "Android allows Spendo to read banking alerts strictly on-device.",
                                    fontSize = 12.sp,
                                    color = SpendoColor.CardDarkTextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SpendoColor.SuccessGreen)
                            )
                        }
                    }
                } else {
                    SpendoCard(contentPadding = 16.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DotGridBadge(
                                    text = "ACTION REQUIRED",
                                    indicatorColor = SpendoColor.AccentRed,
                                    containerColor = SpendoColor.SurfaceVariant,
                                    textColor = SpendoColor.AccentRed
                                )
                                Text(
                                    text = "NOT GRANTED",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendoColor.AccentRed
                                )
                            }

                            Text(
                                text = "Notification Access Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextPrimary
                            )

                            Text(
                                text = "Android requires explicit authorization in System Settings so Spendo can detect incoming debit and credit transaction alerts. Without this permission, background monitoring is disabled.",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = SpendoColor.TextSecondary
                            )

                            SpendoPrimaryButton(
                                text = "Enable Notification Access",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        // Fallback to generic settings if specific action unavailable
                                        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. Master Monitoring Switch
            item {
                DotMatrixText(text = "Detection Pipeline Controls", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                SpendoDarkCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotGridBadge(
                                text = when {
                                    !uiState.isNotificationAccessGranted -> "Blocked (No Permission)"
                                    uiState.isNotificationDetectionEnabled -> "Active Ingestion"
                                    else -> "Paused"
                                },
                                indicatorColor = when {
                                    !uiState.isNotificationAccessGranted -> SpendoColor.AccentRed
                                    uiState.isNotificationDetectionEnabled -> SpendoColor.SuccessGreen
                                    else -> SpendoColor.TextMuted
                                },
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
                                text = "Parse debit and credit notifications from permitted banking and payment apps in local memory.",
                                fontSize = 12.sp,
                                color = SpendoColor.CardDarkTextSecondary
                            )
                        }

                        Switch(
                            checked = uiState.isNotificationDetectionEnabled && uiState.isNotificationAccessGranted,
                            onCheckedChange = onToggleDetection,
                            enabled = uiState.isNotificationAccessGranted,
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

            // 3. Live Ingestion Diagnostics & Statistics
            item {
                DotMatrixText(text = "Live Monitoring Statistics", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                SpendoCard(contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Engine Metrics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextPrimary
                            )
                            Text(
                                text = if (uiState.isNotificationDetectionEnabled && uiState.isNotificationAccessGranted) "LIVE LISTENING" else "IDLE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isNotificationDetectionEnabled && uiState.isNotificationAccessGranted) SpendoColor.SuccessGreen else SpendoColor.TextMuted
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                DotMatrixText(text = "Processed", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                                Text(
                                    text = "${uiState.ingestionStats.totalProcessed}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendoColor.TextPrimary
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                DotMatrixText(text = "Extracted", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                                Text(
                                    text = "${uiState.ingestionStats.extractedTransactions}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendoColor.SuccessGreen
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                DotMatrixText(text = "Duplicates", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                                Text(
                                    text = "${uiState.ingestionStats.duplicatesFiltered}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SpendoColor.TextSecondary
                                )
                            }
                        }

                        val lastProcessedText = uiState.ingestionStats.lastProcessedEpochMs?.let {
                            SimpleDateFormat("HH:mm:ss dd-MMM", Locale.getDefault()).format(Date(it))
                        } ?: "None yet"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DotMatrixText(text = "Last Processed Alert", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                            Text(
                                text = lastProcessedText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SpendoColor.TextPrimary
                            )
                        }

                        uiState.ingestionStats.lastError?.let { err ->
                            Text(
                                text = "Recent Diagnostic Note: $err",
                                fontSize = 11.sp,
                                color = SpendoColor.AccentRed
                            )
                        }
                    }
                }
            }

            // 4. Diagnostic Parser Test
            item {
                DotMatrixText(text = "Interactive Parser Test", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                SpendoCard(contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Test Financial Parser (Safe Sandbox)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.TextPrimary
                        )
                        Text(
                            text = "Paste any notification text to verify field extraction without modifying your financial records.",
                            fontSize = 12.sp,
                            color = SpendoColor.TextSecondary
                        )

                        OutlinedTextField(
                            value = testInputText,
                            onValueChange = { testInputText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = SpendoColor.TextPrimary
                            ),
                            placeholder = { Text("Paste bank debit/credit alert...", fontSize = 12.sp) },
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SpendoColor.Surface,
                                unfocusedContainerColor = SpendoColor.Surface,
                                focusedIndicatorColor = SpendoColor.TextPrimary,
                                unfocusedIndicatorColor = SpendoColor.SurfaceVariant,
                                cursorColor = SpendoColor.TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SpendoPrimaryButton(
                                text = "Run Parser Test",
                                onClick = { onTestParse(testInputText) },
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.testParseResult != null) {
                                SpendoOutlinedButton(
                                    text = "Clear",
                                    onClick = onClearTestResult
                                )
                            }
                        }

                        // Display Parser Result
                        uiState.testParseResult?.let { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SpendoColor.SurfaceVariant)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (result.isSuccess) "PARSER MATCH: ${result.parserUsed}" else "PARSING FAILED",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.isSuccess) SpendoColor.SuccessGreen else SpendoColor.AccentRed
                                    )
                                    Text(
                                        text = result.direction ?: "N/A",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.direction == "CREDIT") SpendoColor.SuccessGreen else SpendoColor.AccentRed
                                    )
                                }

                                if (result.isSuccess) {
                                    result.amountPaise?.let { amount ->
                                        Text(
                                            text = "Amount: ₹%,.2f (%d paise)".format(Locale.ENGLISH, amount / 100.0, amount),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SpendoColor.TextPrimary
                                        )
                                    }
                                    result.reportedBalancePaise?.let { bal ->
                                        Text(
                                            text = "Available Balance: ₹%,.2f (%d paise)".format(Locale.ENGLISH, bal / 100.0, bal),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SpendoColor.TextSecondary
                                        )
                                    }
                                    result.merchant?.let { m ->
                                        Text(
                                            text = "Payee/Merchant: $m",
                                            fontSize = 12.sp,
                                            color = SpendoColor.TextPrimary
                                        )
                                    }
                                    result.bankName?.let { b ->
                                        Text(
                                            text = "Bank Detected: $b",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SpendoColor.TextPrimary
                                        )
                                    }
                                    result.accountSuffix?.let { ac ->
                                        Text(
                                            text = "Account Suffix Extracted: •••• $ac",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SpendoColor.TextSecondary
                                        )
                                    }
                                    result.matchedAccountName?.let { ma ->
                                        Text(
                                            text = "Matched Saved Account: $ma",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SpendoColor.SuccessGreen
                                        )
                                    }
                                    result.matchingStatus?.let { status ->
                                        Text(
                                            text = "Matching Status: $status",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (status == "EXACT_MATCH") SpendoColor.SuccessGreen else SpendoColor.WarningAmber
                                        )
                                    }
                                    result.ambiguousReason?.let { reason ->
                                        Text(
                                            text = "Reason for Ambiguity: $reason",
                                            fontSize = 11.sp,
                                            color = SpendoColor.WarningAmber
                                        )
                                    }
                                    result.confidence?.let { conf ->
                                        Text(
                                            text = "Parser Confidence: %.0f%%".format(Locale.ENGLISH, conf * 100f),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SpendoColor.TextSecondary
                                        )
                                    }
                                    result.referenceNumber?.let { ref ->
                                        Text(
                                            text = "Reference / UTR: $ref",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = SpendoColor.TextSecondary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = result.errorMessage ?: "Alert does not match any recognized financial transaction patterns.",
                                        fontSize = 12.sp,
                                        color = SpendoColor.AccentRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Monitored Banking & Payment Apps Allowlist
            item {
                DotMatrixText(text = "Monitored Banking & Payment Apps", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            if (uiState.monitoredApps.isEmpty()) {
                item {
                    SpendoCard(contentPadding = 14.dp) {
                        Text(
                            text = "Initializing default financial apps...",
                            fontSize = 12.sp,
                            color = SpendoColor.TextSecondary
                        )
                    }
                }
            } else {
                items(uiState.monitoredApps, key = { it.packageName }) { appItem ->
                    SpendoCard(contentPadding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = appItem.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SpendoColor.TextPrimary
                                )
                                Text(
                                    text = appItem.packageName,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SpendoColor.TextSecondary
                                )
                            }

                            Switch(
                                checked = appItem.isEnabled,
                                onCheckedChange = { isChecked ->
                                    onToggleApp(appItem.packageName, isChecked)
                                },
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
            }

            // 5b. Notification Tone Personality Selector
            item {
                DotMatrixText(text = "Notification Voice & Tone", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            item {
                SpendoCard(contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Alert Personality",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.TextPrimary
                        )
                        Text(
                            text = "Choose the communication style for transaction alerts and queue review reminders.",
                            fontSize = 12.sp,
                            color = SpendoColor.TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                com.spendwise.android.notification.NotificationPersonalityTone.WITTY to "Witty 💡",
                                com.spendwise.android.notification.NotificationPersonalityTone.FRIENDLY to "Friendly 👋",
                                com.spendwise.android.notification.NotificationPersonalityTone.MINIMAL to "Minimal ⏱️"
                            ).forEach { (tone, label) ->
                                val isSelected = uiState.notificationTone == tone
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) SpendoColor.TextPrimary else SpendoColor.Surface)
                                        .border(1.dp, if (isSelected) SpendoColor.TextPrimary else SpendoColor.Divider, RoundedCornerShape(8.dp))
                                        .clickable { onToneChange(tone) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SpendoColor.Background else SpendoColor.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Daily Review Reminders Header
            item {
                DotMatrixText(text = "Scheduled Review Reminders", fontSize = 11.sp, color = SpendoColor.TextSecondary)
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
                                SpendoReminderScheduler.updatePreferences(
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
                                SpendoReminderScheduler.updatePreferences(
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

                        SpendoOutlinedButton(
                            text = "Test Now",
                            onClick = {
                                SpendoNotificationHelper.showTestNotification(context)
                                testNotificationSent = true
                            }
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
                            text = "Spendo filters notifications exclusively in local memory. OTPs, personal messages, and full message bodies are never logged or stored. Only financial transactions matching recognized formats are saved to your local database.",
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
