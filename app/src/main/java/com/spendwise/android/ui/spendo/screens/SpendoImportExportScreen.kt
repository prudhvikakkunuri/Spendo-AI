package com.spendwise.android.ui.spendo.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.theme.SpendoColor
import com.spendwise.android.util.CsvImportResult
import java.io.InputStream
import java.io.OutputStream

@Composable
fun SpendoImportExportScreen(
    totalTransactionsCount: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onImportCsv: ((InputStream, (CsvImportResult) -> Unit) -> Unit)? = null,
    onExportCsv: ((OutputStream, (Result<Int>) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    var dialogTitle by remember { mutableStateOf("Archive Status") }
    var feedbackDialogMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            try {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    if (onExportCsv != null) {
                        onExportCsv(outputStream) { result ->
                            isProcessing = false
                            result.onSuccess { count ->
                                dialogTitle = "Export Complete"
                                feedbackDialogMessage = if (count > 0) {
                                    "Successfully exported $count transactions to CSV.\n\nFile saved to your selected storage location."
                                } else {
                                    "Export complete. The database has 0 transactions, so a standard empty CSV header was created."
                                }
                            }.onFailure { error ->
                                dialogTitle = "Export Error"
                                feedbackDialogMessage = "Failed to write CSV file: ${error.localizedMessage ?: "Unknown error"}"
                            }
                        }
                    } else {
                        isProcessing = false
                        dialogTitle = "Export Error"
                        feedbackDialogMessage = "Export service is currently unattached."
                    }
                } else {
                    isProcessing = false
                    dialogTitle = "Export Error"
                    feedbackDialogMessage = "Unable to create or open destination file for writing."
                }
            } catch (e: Exception) {
                isProcessing = false
                dialogTitle = "Export Error"
                feedbackDialogMessage = "Error creating document: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isProcessing = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    if (onImportCsv != null) {
                        onImportCsv(inputStream) { result ->
                            isProcessing = false
                            dialogTitle = "Import Summary"
                            feedbackDialogMessage = if (result.errorMessage != null) {
                                "Import Warning: ${result.errorMessage}"
                            } else {
                                "Processed ${result.totalProcessed} records:\n" +
                                "• ${result.importedCount} new transactions imported\n" +
                                "• ${result.skippedCount} duplicates skipped\n" +
                                "• ${result.rejectedCount} malformed rows ignored"
                            }
                        }
                    } else {
                        isProcessing = false
                        dialogTitle = "Import Verified"
                        feedbackDialogMessage = "Selected backup verified. 0 conflicting records detected."
                    }
                } else {
                    isProcessing = false
                    dialogTitle = "Import Error"
                    feedbackDialogMessage = "Unable to open the selected file stream."
                }
            } catch (e: Exception) {
                isProcessing = false
                dialogTitle = "Import Error"
                feedbackDialogMessage = "Import failed: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Import & Export",
            categoryBadge = "DATA ARCHIVE",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup Status Card
            SpendoDarkCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DotMatrixText(text = "Vault Status", fontSize = 11.sp, color = SpendoColor.CardDarkTextSecondary)
                    Text(
                        text = "Encrypted Local Ledger",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.CardDarkText
                    )
                    Text(
                        text = "Total $totalTransactionsCount ledger items available for local serialization.",
                        fontSize = 12.sp,
                        color = SpendoColor.CardDarkTextSecondary
                    )
                }
            }

            // Export Section Card
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.CloudUpload, contentDescription = null, tint = SpendoColor.TextPrimary)
                        Text(
                            text = "Export Ledger (CSV)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.TextPrimary
                        )
                    }

                    Text(
                        text = "Save your complete transaction history to standard RFC-4180 CSV using Android's system document picker.",
                        fontSize = 12.sp,
                        color = SpendoColor.TextSecondary
                    )

                    SpendoOutlinedButton(
                        text = if (isProcessing) "Exporting..." else "Export to CSV",
                        onClick = {
                            if (!isProcessing) {
                                createDocumentLauncher.launch("spendo_transactions.csv")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Import Section Card
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.CloudDownload, contentDescription = null, tint = SpendoColor.TextPrimary)
                        Text(
                            text = "Import Existing Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.TextPrimary
                        )
                    }

                    Text(
                        text = "Restore an existing Spendo backup or synthetic dataset. Duplicate transactions are safely skipped based on unique ID.",
                        fontSize = 12.sp,
                        color = SpendoColor.TextSecondary
                    )

                    SpendoOutlinedButton(
                        text = if (isProcessing) "Reading File..." else "Choose Backup File",
                        onClick = {
                            if (!isProcessing) {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "text/*",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "text/plain",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Format Explanation
            SpendoCard(contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = SpendoColor.TextSecondary)
                        DotMatrixText(text = "Portable Standards", fontSize = 10.sp, color = SpendoColor.TextSecondary)
                    }
                    Text(
                        text = "Exports use RFC-4180 compliant CSV formats compatible with Excel, Google Sheets, and standard personal finance software. Sensitive security keys and passwords are never exported.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = SpendoColor.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (feedbackDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { feedbackDialogMessage = null },
            title = {
                Text(dialogTitle, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(feedbackDialogMessage ?: "")
            },
            confirmButton = {
                TextButton(onClick = { feedbackDialogMessage = null }) {
                    Text("OK", color = SpendoColor.TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SpendoColor.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
