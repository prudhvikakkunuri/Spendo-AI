package com.spendwise.android.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spendwise.android.data.db.entity.MonitoredAppEntity
import com.spendwise.android.data.repository.MonitoredAppRepository
import com.spendwise.android.ui.common.SpendWiseTopBar
import com.spendwise.android.ui.theme.CharcoalBorder
import com.spendwise.android.ui.theme.CharcoalDark
import com.spendwise.android.ui.theme.LimeAccent
import com.spendwise.android.ui.theme.SlateTextMuted
import com.spendwise.android.ui.theme.SlateTextSecondary

@Composable
fun MonitoredAppsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MonitoredAppsViewModel? = null,
    modifier: Modifier = Modifier
) {
    var showAddAppDialog by remember { mutableStateOf(false) }

    val isPausedState = if (viewModel != null) {
        val paused by viewModel.isDetectionPaused.collectAsState()
        paused
    } else {
        var fallbackPaused by remember { mutableStateOf(false) }
        fallbackPaused
    }

    val appsList = if (viewModel != null) {
        val observedApps by viewModel.apps.collectAsState()
        observedApps
    } else {
        remember {
            MonitoredAppRepository.DEFAULT_KNOWN_FINANCIAL_APPS
        }
    }

    // Local state for fallback preview editing if viewModel is null
    val localFallbackMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            MonitoredAppRepository.DEFAULT_KNOWN_FINANCIAL_APPS.forEach { app ->
                put(app.packageName, app.isEnabled)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SpendWiseTopBar(
            title = "Monitored Apps",
            subtitle = "Choose source apps for detection",
            onNavigateBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Master Pause Switch Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pause All Detection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Temporarily suspend notification reading without resetting permissions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary
                            )
                        }

                        Switch(
                            checked = isPausedState,
                            onCheckedChange = { checked ->
                                viewModel?.setDetectionPaused(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CharcoalDark,
                                checkedTrackColor = LimeAccent
                            )
                        )
                    }
                }
            }

            item {
                // Allowlist Explanation
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = LimeAccent,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        Text(
                            text = "SpendWise strictly filters notifications by package. Only apps toggled ON below are inspected in memory. All other apps are discarded immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MONITORED SOURCES (${appsList.count { it.isEnabled }} ACTIVE)",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateTextMuted
                    )

                    TextButton(onClick = { showAddAppDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = LimeAccent,
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Add App", color = LimeAccent, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            items(appsList, key = { it.packageName }) { app ->
                val isChecked = if (viewModel != null) app.isEnabled else (localFallbackMap[app.packageName] ?: app.isEnabled)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextMuted
                            )
                        }

                        Switch(
                            checked = isChecked,
                            enabled = !isPausedState,
                            onCheckedChange = { checked ->
                                if (viewModel != null) {
                                    viewModel.toggleApp(app.packageName, checked)
                                } else {
                                    localFallbackMap[app.packageName] = checked
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

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddAppDialog) {
        var newAppName by remember { mutableStateOf("") }
        var newPackageName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAppDialog = false },
            title = {
                Text(
                    text = "Add Banking / UPI App",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter the application name and Android package name (e.g. com.bank.app) to allowlist:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )
                    OutlinedTextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("App Name (e.g. Kotak 811)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPackageName,
                        onValueChange = { newPackageName = it },
                        label = { Text("Package Name (e.g. com.msf.kbank.mobile)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAppName.isNotBlank() && newPackageName.isNotBlank()) {
                            viewModel?.addCustomApp(newPackageName, newAppName)
                            showAddAppDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LimeAccent, contentColor = CharcoalDark)
                ) {
                    Text("Add App", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
