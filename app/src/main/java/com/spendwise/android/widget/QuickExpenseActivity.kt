package com.spendwise.android.widget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.spendwise.android.SpendWiseApplication
import com.spendwise.android.data.db.AppDatabase
import com.spendwise.android.data.db.entity.BankAccountEntity
import com.spendwise.android.domain.voice.VoiceFinancialParser
import com.spendwise.android.domain.voice.VoiceIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight floating dialog Activity for widget-initiated Voice and Manual expense entries.
 * Operates purely as a compact overlay without opening the main dashboard or navigation hierarchy.
 */
class QuickExpenseActivity : ComponentActivity() {

    companion object {
        const val ACTION_VOICE = "com.spendwise.android.widget.ACTION_VOICE"
        const val ACTION_MANUAL = "com.spendwise.android.widget.ACTION_MANUAL"
    }

    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isVoiceAction = intent?.action == ACTION_VOICE

        setContent {
            QuickExpenseDialogContent(
                isInitialVoice = isVoiceAction,
                onClose = { finish() },
                onLaunchSpeech = { listener -> startListening(listener) },
                onStopSpeech = { stopListening() }
            )
        }
    }

    private fun startListening(listener: RecognitionListener) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            listener.onError(SpeechRecognizer.ERROR_CLIENT)
            return
        }

        try {
            stopListening()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(listener)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                startListening(intent)
            }
        } catch (_: Exception) {
            listener.onError(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        } finally {
            speechRecognizer = null
        }
    }

    override fun onDestroy() {
        stopListening()
        super.onDestroy()
    }
}

private enum class QuickExpenseScreenState {
    RECORDING,
    CONFIRMATION,
    MANUAL_ENTRY,
    ERROR
}

private val AVAILABLE_CATEGORIES = listOf(
    "Food & Dining",
    "Transit",
    "Groceries",
    "Shopping",
    "Entertainment",
    "Bills & Utilities",
    "Subscriptions",
    "Health",
    "Other"
)

@Composable
private fun QuickExpenseDialogContent(
    isInitialVoice: Boolean,
    onClose: () -> Unit,
    onLaunchSpeech: (RecognitionListener) -> Unit,
    onStopSpeech: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var screenState by remember {
        mutableStateOf(if (isInitialVoice) QuickExpenseScreenState.RECORDING else QuickExpenseScreenState.MANUAL_ENTRY)
    }

    var recognizedTranscript by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Listening... Speak your expense clearly.") }
    var isMicrophoneActive by remember { mutableStateOf(false) }

    // Draft Transaction Fields
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }
    var selectedBank by remember { mutableStateOf<BankAccountEntity?>(null) }
    var dateEpochMs by remember { mutableStateOf(System.currentTimeMillis()) }

    var isEditing by remember { mutableStateOf(!isInitialVoice) }
    var isSaving by remember { mutableStateOf(false) }
    var bankAccounts by remember { mutableStateOf<List<BankAccountEntity>>(emptyList()) }

    // Load bank accounts from Room
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val accounts = db.bankAccountDao().getAllBankAccountsDirect()
                bankAccounts = accounts
                if (selectedBank == null && accounts.isNotEmpty()) {
                    selectedBank = accounts.firstOrNull { it.isPrimary } ?: accounts.firstOrNull()
                }
            } catch (_: Exception) {
            }
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isMicrophoneActive = true
                statusMessage = "Listening for expense... (e.g. \"Spent 250 on lunch today\")"
            }

            override fun onBeginningOfSpeech() {
                statusMessage = "Listening..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isMicrophoneActive = false
                statusMessage = "Processing your expense..."
            }

            override fun onError(error: Int) {
                isMicrophoneActive = false
                statusMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please tap Try Again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out. Please try again."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Please check microphone."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                    else -> "Speech recognition unavailable. Tap Manual Entry below."
                }
                screenState = QuickExpenseScreenState.ERROR
            }

            override fun onResults(results: Bundle?) {
                isMicrophoneActive = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val transcript = matches?.firstOrNull() ?: ""
                recognizedTranscript = transcript

                if (transcript.isBlank()) {
                    statusMessage = "No words recognized. Please try again."
                    screenState = QuickExpenseScreenState.ERROR
                    return
                }

                val intent = VoiceFinancialParser.parseSpeech(transcript)
                when (intent) {
                    is VoiceIntent.AddExpense -> {
                        amountText = (intent.amountPaise / 100.0).toString().removeSuffix(".0")
                        merchantText = intent.merchant
                        selectedCategory = intent.category
                        dateEpochMs = intent.occurredAtEpochMs

                        // Match spoken bank if found
                        val matched = bankAccounts.find {
                            it.bankName.contains(intent.bankName, ignoreCase = true) ||
                                    (it.accountNickname != null && it.accountNickname.contains(intent.bankName, ignoreCase = true))
                        }
                        if (matched != null) {
                            selectedBank = matched
                        }

                        isEditing = false
                        screenState = QuickExpenseScreenState.CONFIRMATION
                    }
                    is VoiceIntent.AmbiguousExpense -> {
                        statusMessage = intent.promptForMissing
                        screenState = QuickExpenseScreenState.ERROR
                    }
                    else -> {
                        statusMessage = "Could not detect expense details in: \"$transcript\". Try: \"Spent 250 on lunch\""
                        screenState = QuickExpenseScreenState.ERROR
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partials?.firstOrNull()?.let {
                    recognizedTranscript = it
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onLaunchSpeech(recognitionListener)
        } else {
            statusMessage = "Microphone permission is required for voice entry."
            screenState = QuickExpenseScreenState.ERROR
        }
    }

    LaunchedEffect(isInitialVoice) {
        if (isInitialVoice) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPerm) {
                onLaunchSpeech(recognitionListener)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopSpeech()
        }
    }

    fun saveExpenseToDb() {
        val amtPaise = parseAmountToPaise(amountText)
        if (amtPaise == null || amtPaise <= 0L) {
            Toast.makeText(context, "Please enter a valid expense amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (merchantText.isBlank()) {
            merchantText = "Quick Expense"
        }

        val targetBankName = selectedBank?.bankName ?: "Primary Bank"
        val targetBankId = selectedBank?.id

        isSaving = true
        coroutineScope.launch {
            try {
                val app = SpendWiseApplication.instance
                app.spendoFinancialRepository.addExpense(
                    merchant = merchantText.trim(),
                    amountPaise = amtPaise,
                    category = selectedCategory,
                    bankAccountName = targetBankName,
                    note = "Widget Quick Entry",
                    occurredAtEpochMs = dateEpochMs,
                    bankAccountId = targetBankId
                )

                // Refresh all home-screen widgets immediately
                SpendoWidgetProvider.updateAllWidgets(context)

                val formattedAmt = "₹%,.2f".format(Locale.ENGLISH, amtPaise / 100.0)
                Toast.makeText(context, "✓ Saved $formattedAmt for $merchantText", Toast.LENGTH_SHORT).show()
                onClose()
            } catch (e: Exception) {
                isSaving = false
                Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Outer backdrop box to dim screen and detect outside taps
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isSaving) onClose()
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating Card
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Prevent dismissing when tapping card body */ }
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isMicrophoneActive) Color(0xFFFF453A) else Color(0xFFB7F34A))
                    )
                    Text(
                        text = if (screenState == QuickExpenseScreenState.RECORDING) "VOICE EXPENSE ENTRY" else "CONFIRM EXPENSE",
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "✕",
                    color = Color(0xFF888888),
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { if (!isSaving) onClose() }
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (screenState) {
                QuickExpenseScreenState.RECORDING -> {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isMicrophoneActive) Color(0x33FF453A) else Color(0x22B7F34A))
                            .border(
                                2.dp,
                                if (isMicrophoneActive) Color(0xFFFF453A) else Color(0xFFB7F34A),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isMicrophoneActive) "🎙️" else "⏳",
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = statusMessage,
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )

                    if (recognizedTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"$recognizedTranscript\"",
                            color = Color(0xFFB7F34A),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222))
                                .clickable {
                                    onStopSpeech()
                                    screenState = QuickExpenseScreenState.MANUAL_ENTRY
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Switch to Manual",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222))
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFFAAAAAA),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                QuickExpenseScreenState.ERROR -> {
                    Text(
                        text = "⚠️ $statusMessage",
                        color = Color(0xFFFF7B72),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF222222))
                                .clickable {
                                    screenState = QuickExpenseScreenState.RECORDING
                                    onLaunchSpeech(recognitionListener)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Try Again", color = Color.White, fontSize = 13.sp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFB7F34A))
                                .clickable {
                                    screenState = QuickExpenseScreenState.MANUAL_ENTRY
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Manual Entry", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.8f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E1E))
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "Cancel", color = Color(0xFFAAAAAA), fontSize = 13.sp)
                        }
                    }
                }

                QuickExpenseScreenState.CONFIRMATION, QuickExpenseScreenState.MANUAL_ENTRY -> {
                    // Amount Display / Input
                    if (!isEditing && screenState == QuickExpenseScreenState.CONFIRMATION) {
                        val parsedPaise = parseAmountToPaise(amountText) ?: 0L
                        Text(
                            text = "₹%,.2f".format(Locale.ENGLISH, parsedPaise / 100.0),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Text(
                            text = merchantText.ifBlank { "Expense" },
                            fontSize = 16.sp,
                            color = Color(0xFFCCCCCC),
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Amount (₹)", color = Color(0xFF888888)) },
                            placeholder = { Text("e.g. 250", color = Color(0xFF555555)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFB7F34A),
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = merchantText,
                            onValueChange = { merchantText = it },
                            label = { Text("Merchant / Description", color = Color(0xFF888888)) },
                            placeholder = { Text("e.g. Lunch at Cafe", color = Color(0xFF555555)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFB7F34A),
                                unfocusedBorderColor = Color(0xFF333333),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Selector
                    Text(
                        text = "CATEGORY",
                        color = Color(0xFF777777),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(AVAILABLE_CATEGORIES) { cat ->
                            val isSelected = cat.equals(selectedCategory, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) Color(0xFFB7F34A) else Color(0xFF222222))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.Black else Color(0xFFCCCCCC),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bank Account Selector
                    Text(
                        text = "PAYMENT ACCOUNT",
                        color = Color(0xFF777777),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (bankAccounts.isEmpty()) {
                        Text(
                            text = "Primary Bank (Default)",
                            color = Color(0xFF888888),
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(bankAccounts) { account ->
                                val isSelected = selectedBank?.id == account.id
                                val name = account.accountNickname ?: account.bankName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Color(0xFF333333) else Color(0xFF1E1E1E))
                                        .border(
                                            1.dp,
                                            if (isSelected) Color(0xFFB7F34A) else Color(0xFF2A2A2A),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { selectedBank = account }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "$name (${account.maskedAccountNumber})",
                                        color = if (isSelected) Color(0xFFB7F34A) else Color(0xFF888888),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Indicator
                    val formattedDate = remember(dateEpochMs) {
                        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(dateEpochMs))
                    }
                    Text(
                        text = "Date: $formattedDate",
                        color = Color(0xFF666666),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Confirm & Save
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSaving) Color(0xFF555555) else Color(0xFFB7F34A))
                                .clickable(enabled = !isSaving) { saveExpenseToDb() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (screenState == QuickExpenseScreenState.CONFIRMATION) "Confirm & Save" else "Save Expense",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Edit Button (only shown in confirmation mode if not already editing)
                        if (!isEditing && screenState == QuickExpenseScreenState.CONFIRMATION) {
                            Box(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF222222))
                                    .clickable(enabled = !isSaving) { isEditing = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Edit",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Cancel Button
                        Box(
                            modifier = Modifier
                                .weight(0.8f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E1E))
                                .clickable(enabled = !isSaving) { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFFAAAAAA),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseAmountToPaise(input: String): Long? {
    val clean = input.trim().replace(",", "").replace("₹", "")
    if (clean.isBlank()) return null
    return try {
        val parts = clean.split(".")
        val rupees = parts[0].toLong()
        val paise = if (parts.size > 1) parts[1].padEnd(2, '0').take(2).toLong() else 0L
        (rupees * 100L) + paise
    } catch (_: Exception) {
        null
    }
}
