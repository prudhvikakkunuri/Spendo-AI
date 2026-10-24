package com.spendwise.android.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.data.repository.BudgetRepository
import com.spendwise.android.data.repository.TransactionRepository
import com.spendwise.android.domain.model.Transaction
import com.spendwise.android.ui.theme.CharcoalBorder
import com.spendwise.android.ui.theme.CharcoalDark
import com.spendwise.android.ui.theme.CreditGreen
import com.spendwise.android.ui.theme.DebitRed
import com.spendwise.android.ui.theme.LimeAccent
import com.spendwise.android.ui.theme.SlateTextMuted
import com.spendwise.android.ui.theme.SlateTextSecondary
import com.spendwise.android.util.CsvExporter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    repository: TransactionRepository? = null,
    budgetRepository: BudgetRepository? = null,
    onNavigateToNotificationSetup: () -> Unit,
    onNavigateToMonitoredApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTransactions = repository?.allTransactions?.collectAsState(initial = emptyList())?.value ?: emptyList()

    val app = context.applicationContext as? com.spendwise.android.SpendWiseApplication
    val settingsRepo = app?.notificationSettingsRepository
    val accessMgr = app?.notificationAccessManager
    val monitoredRepo = app?.monitoredAppRepository

    var fallbackDetectionPaused by remember { mutableStateOf(false) }
    val isDetectionPaused = settingsRepo?.isDetectionPaused?.collectAsState()?.value ?: fallbackDetectionPaused
    val isAccessGranted = accessMgr?.isNotificationAccessGranted() ?: false
    val monitoredApps = monitoredRepo?.allMonitoredAppsFlow?.collectAsState(initial = emptyList())?.value ?: emptyList()
    val activeMonitoredCount = if (monitoredApps.isNotEmpty()) monitoredApps.count { it.isEnabled } else 9

    var showClearDataDialog by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    var clearDataCompleted by remember { mutableStateOf(false) }

    // Android Document Picker for saving CSV to user's chosen folder
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        CsvExporter.writeCsvToStream(allTransactions, outputStream)
                    }
                    exportStatusMessage = "Successfully exported ${allTransactions.size} transactions to CSV."
                } catch (e: Exception) {
                    exportStatusMessage = "Export failed: ${e.localizedMessage}"
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Notification & Monitored Apps Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DETECTION CONTROLS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                SettingsNavRow(
                    icon = Icons.Default.Notifications,
                    title = "Notification Access",
                    subtitle = if (isAccessGranted) "Enabled (Listening for alerts)" else "Action Required (Tap to configure)",
                    onClick = onNavigateToNotificationSetup
                )

                Spacer(modifier = Modifier.height(12.dp))

                SettingsNavRow(
                    icon = Icons.Default.Security,
                    title = "Monitored Apps Allowlist",
                    subtitle = "$activeMonitoredCount active payment & banking sources",
                    onClick = onNavigateToMonitoredApps
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pause All Ingestion",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Halt background processing temporarily",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }

                    Switch(
                        checked = isDetectionPaused,
                        onCheckedChange = { checked ->
                            if (settingsRepo != null) {
                                settingsRepo.setDetectionPaused(checked)
                            } else {
                                fallbackDetectionPaused = checked
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CharcoalDark,
                            checkedTrackColor = LimeAccent
                        )
                    )
                }
            }
        }

        // Data & Export Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "DATA MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                SettingsNavRow(
                    icon = Icons.Default.Download,
                    title = "Export Ledger to CSV",
                    subtitle = "Save ${allTransactions.size} records using Android document picker",
                    onClick = {
                        val fileName = "spendwise_ledger_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date())}.csv"
                        createDocumentLauncher.launch(fileName)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                SettingsNavRow(
                    icon = Icons.Default.Share,
                    title = "Share CSV Directly",
                    subtitle = "Share formatted transaction ledger via system share sheet",
                    onClick = {
                        shareCsvText(context, CsvExporter.generateCsv(allTransactions))
                    }
                )

                if (exportStatusMessage != null) {
                    Text(
                        text = exportStatusMessage!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = LimeAccent,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SettingsNavRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear All Local Data",
                    subtitle = "Irreversibly erase transactions, budgets, commitments, and dedupe cache",
                    iconTint = DebitRed,
                    onClick = { showClearDataDialog = true }
                )

                if (clearDataCompleted) {
                    Text(
                        text = "All local database transactions erased.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CreditGreen,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Privacy Guarantees Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = LimeAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Privacy Guarantees",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "• No bank account credentials or payment API connections\n• 100% on-device deterministic processing; no cloud sync\n• Zero analytics trackers, ad SDKs, or external LLM APIs\n• Raw notification bodies are never persisted in logs or database",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary,
                    lineHeight = 20.sp
                )
            }
        }

        // About SpendWise
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SpendWise Android",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Version 1.0.0 • Phase 2 Local Database Ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary
                )
                Text(
                    text = "Persisting via Room Database (SQLite) with StateFlow",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = DebitRed
                )
            },
            title = {
                Text("Erase All Data?")
            },
            text = {
                Text("This action will permanently delete all transaction records, budget settings, commitments, and deduplication hashes. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository?.clearAllTransactions()
                            budgetRepository?.clearBudgetData()
                            settingsRepo?.setDetectionPaused(false)
                            showClearDataDialog = false
                            clearDataCompleted = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DebitRed)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun shareCsvText(context: Context, csvText: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, csvText)
        putExtra(Intent.EXTRA_TITLE, "SpendWise_Ledger.csv")
        type = "text/csv"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share SpendWise Ledger CSV")
    context.startActivity(shareIntent)
}

@Composable
private fun SettingsNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = LimeAccent,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextSecondary
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = SlateTextMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}
