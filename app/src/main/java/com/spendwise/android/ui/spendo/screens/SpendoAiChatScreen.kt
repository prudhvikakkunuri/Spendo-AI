package com.spendwise.android.ui.spendo.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.spendwise.android.domain.voice.VoiceFinancialParser
import com.spendwise.android.domain.voice.VoiceIntent
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoMascot
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.model.PendingExpenseConfirmation
import com.spendwise.android.ui.spendo.model.SpendoChatMessage
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoTheme
import java.util.Locale

enum class VoiceInputState {
    IDLE,
    LISTENING,
    PROCESSING,
    REVIEW,
    ERROR
}

@Composable
fun SpendoAiChatScreen(
    uiState: SpendoUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onNavigateToAiConfig: () -> Unit,
    onConfirmExpense: (PendingExpenseConfirmation, String) -> Unit = { _, _ -> },
    onDismissExpense: (String) -> Unit = {},
    onClearChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = SpendoTheme.colors
    val listState = rememberLazyListState()

    var voiceState by remember { mutableStateOf(VoiceInputState.IDLE) }
    var voiceTranscript by remember { mutableStateOf<String?>(null) }
    var voiceErrorMessage by remember { mutableStateOf<String?>(null) }
    var rmsAudioLevel by remember { mutableFloatStateOf(0f) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // SpeechRecognizer state holder
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val stopSpeechRecognition = {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        if (voiceState == VoiceInputState.LISTENING) {
            voiceState = VoiceInputState.IDLE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopSpeechRecognition()
        }
    }

    val startListening = {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            voiceState = VoiceInputState.ERROR
            voiceErrorMessage = "Speech recognition service is not available on this device."
        } else {
            stopSpeechRecognition()
            voiceErrorMessage = null
            voiceTranscript = null
            voiceState = VoiceInputState.LISTENING

            try {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        voiceState = VoiceInputState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {
                        voiceState = VoiceInputState.LISTENING
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        rmsAudioLevel = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        voiceState = VoiceInputState.PROCESSING
                    }

                    override fun onError(error: Int) {
                        voiceState = VoiceInputState.ERROR
                        voiceErrorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly and try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Tap retry to speak."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required for voice input."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue reaching speech recognition service."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Retrying..."
                            else -> "Speech recognition error ($error). Tap to retry."
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull()?.trim()
                        if (!spoken.isNullOrBlank()) {
                            voiceTranscript = spoken
                            voiceState = VoiceInputState.REVIEW
                        } else {
                            voiceState = VoiceInputState.ERROR
                            voiceErrorMessage = "No speech detected. Please speak clearly and retry."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = partials?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            voiceTranscript = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                recognizer.startListening(intent)
            } catch (e: Exception) {
                voiceState = VoiceInputState.ERROR
                voiceErrorMessage = "Could not initialize microphone: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            voiceState = VoiceInputState.ERROR
            voiceErrorMessage = "Microphone permission was denied. Please grant permission in App Settings."
        }
    }

    val onMicClicked = {
        if (voiceState == VoiceInputState.LISTENING) {
            stopSpeechRecognition()
            voiceState = VoiceInputState.IDLE
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    val suggestedQuestions = listOf(
        "How much did I spend today?",
        "Show my expenses this month.",
        "What is my biggest expense?",
        "Which category costs me the most?",
        "How much money did I save?",
        "Analyze my spending habits.",
        "Show my recent transactions.",
        "How much is left in my budget?",
        "Compare my spending with last month",
        "How can I reduce my spending next month?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Dedicated Header with Spendo Mascot, Clear Action & Configuration Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpendoMascot(size = 40.dp, isActive = uiState.aiConfig.isConfigured)

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (uiState.aiConfig.isConfigured) colors.successGreen else colors.accentRed)
                        )
                        DotMatrixText(
                            text = if (uiState.aiConfig.isConfigured) "AI ONLINE • ${uiState.aiConfig.provider.displayName}" else "OFFLINE ASSISTANT",
                            fontSize = 10.sp,
                            color = colors.textSecondary
                        )
                    }

                    Text(
                        text = "Spendo AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.chatMessages.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Clear Conversation",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateToAiConfig,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Configure AI Provider",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Live Chat Message Feed
        if (uiState.chatMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpendoMascot(size = 64.dp, isActive = uiState.aiConfig.isConfigured)
                    Text(
                        text = "Your AI Financial Agent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Speak naturally to log an expense, ask about bank balances, or query category spending.\nTry tapping a suggestion below or hit the Mic button.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }

                items(uiState.chatMessages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onConfirmExpense = onConfirmExpense,
                        onDismissExpense = onDismissExpense
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Voice Assistant Active Recording Bar
        AnimatedVisibility(visible = voiceState != VoiceInputState.IDLE) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceDark)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    .size(12.dp)
                                    .scale(if (voiceState == VoiceInputState.LISTENING) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        when (voiceState) {
                                            VoiceInputState.LISTENING -> colors.accentRed
                                            VoiceInputState.PROCESSING -> colors.warningAmber
                                            VoiceInputState.REVIEW -> colors.successGreen
                                            VoiceInputState.ERROR -> colors.accentRed
                                            else -> colors.textSecondary
                                        }
                                    )
                            )
                            Text(
                                text = when (voiceState) {
                                    VoiceInputState.LISTENING -> "LISTENING... SPEAK NOW"
                                    VoiceInputState.PROCESSING -> "PROCESSING SPEECH..."
                                    VoiceInputState.REVIEW -> "DETECTED SPEECH"
                                    VoiceInputState.ERROR -> "VOICE INPUT ERROR"
                                    else -> "VOICE AGENT"
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (voiceState) {
                                    VoiceInputState.LISTENING -> colors.accentRed
                                    VoiceInputState.ERROR -> colors.accentRed
                                    VoiceInputState.REVIEW -> colors.cardDarkText
                                    else -> colors.cardDarkTextSecondary
                                }
                            )
                        }

                        IconButton(
                            onClick = {
                                stopSpeechRecognition()
                                voiceState = VoiceInputState.IDLE
                                voiceTranscript = null
                                voiceErrorMessage = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = colors.cardDarkTextSecondary
                            )
                        }
                    }

                    // Content based on Voice State
                    when (voiceState) {
                        VoiceInputState.LISTENING -> {
                            Text(
                                text = voiceTranscript ?: "Listening for financial command or expense...\n(e.g., 'I spent 250 rupees on lunch through Kotak today')",
                                fontSize = 13.sp,
                                color = colors.cardDarkText,
                                lineHeight = 18.sp
                            )
                        }
                        VoiceInputState.PROCESSING -> {
                            Text(
                                text = voiceTranscript ?: "Analyzing recognized audio...",
                                fontSize = 13.sp,
                                color = colors.cardDarkText,
                                lineHeight = 18.sp
                            )
                        }
                        VoiceInputState.REVIEW -> {
                            val transcriptText = voiceTranscript ?: ""
                            val parsedIntent = VoiceFinancialParser.parseSpeech(transcriptText)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "\"$transcriptText\"",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.cardDarkText,
                                    lineHeight = 18.sp
                                )

                                when (parsedIntent) {
                                    is VoiceIntent.AddExpense -> {
                                        Text(
                                            text = "Detected Expense: ₹%.2f • %s • %s".format(
                                                parsedIntent.amountPaise / 100.0,
                                                parsedIntent.merchant,
                                                parsedIntent.bankName
                                            ),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.cardDarkTextSecondary
                                        )
                                    }
                                    is VoiceIntent.AddIncome -> {
                                        Text(
                                            text = "Detected Income: ₹%.2f • %s • %s".format(
                                                parsedIntent.amountPaise / 100.0,
                                                parsedIntent.source,
                                                parsedIntent.bankName
                                            ),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.cardDarkTextSecondary
                                        )
                                    }
                                    else -> {}
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SpendoOutlinedButton(
                                        text = "Send to Spendo",
                                        onClick = {
                                            val textToSend = voiceTranscript ?: ""
                                            stopSpeechRecognition()
                                            voiceState = VoiceInputState.IDLE
                                            voiceTranscript = null
                                            if (textToSend.isNotBlank()) {
                                                onSendMessage(textToSend)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    SpendoOutlinedButton(
                                        text = "Edit Text",
                                        onClick = {
                                            val textToEdit = voiceTranscript ?: ""
                                            stopSpeechRecognition()
                                            voiceState = VoiceInputState.IDLE
                                            voiceTranscript = null
                                            onInputChange(textToEdit)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        VoiceInputState.ERROR -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = voiceErrorMessage ?: "Unable to complete voice recognition.",
                                    fontSize = 12.sp,
                                    color = colors.accentRed,
                                    lineHeight = 16.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SpendoOutlinedButton(
                                        text = "Retry",
                                        onClick = { startListening() },
                                        modifier = Modifier.weight(1f),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )

                                    SpendoOutlinedButton(
                                        text = "Dismiss",
                                        onClick = {
                                            voiceState = VoiceInputState.IDLE
                                            voiceErrorMessage = null
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }

        // Horizontal Quick Prompts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedQuestions.forEach { question ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(20.dp))
                        .clickable { onSendMessage(question) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = question,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSecondary
                    )
                }
            }
        }

        // Bottom Input Row with Text Field, Mic Button & Send Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.chatInputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Ask Spendo or tap Mic to speak...",
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.background,
                    unfocusedContainerColor = colors.background,
                    focusedIndicatorColor = colors.textPrimary,
                    unfocusedIndicatorColor = colors.divider,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            // Real Android SpeechRecognizer Microphone Button
            IconButton(
                onClick = onMicClicked,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (voiceState == VoiceInputState.LISTENING) colors.accentRed else colors.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (voiceState == VoiceInputState.LISTENING) Icons.Outlined.Stop else Icons.Outlined.Mic,
                    contentDescription = "Voice Assistant",
                    tint = if (voiceState == VoiceInputState.LISTENING) Color.White else colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Send Button
            IconButton(
                onClick = { onSendMessage(null) },
                enabled = uiState.chatInputText.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (uiState.chatInputText.isNotBlank()) colors.surfaceDark else colors.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (uiState.chatInputText.isNotBlank()) (if (colors.isDark) colors.textPrimary else colors.textInverse) else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text("Clear Conversation", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to clear your AI chat history? This only clears the messages view and does not delete your ledger transactions.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearChat()
                    }
                ) {
                    Text("Clear", color = colors.accentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ChatBubble(
    message: SpendoChatMessage,
    onConfirmExpense: (PendingExpenseConfirmation, String) -> Unit,
    onDismissExpense: (String) -> Unit
) {
    val colors = SpendoTheme.colors
    val isUser = message.sender == "USER"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
        ) {
            if (!isUser) {
                SpendoMascot(size = 28.dp, isActive = true)
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(if (isUser) colors.surfaceDark else colors.surface)
                    .border(
                        1.dp,
                        if (isUser) colors.borderDark else colors.divider,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isUser) (if (colors.isDark) colors.textPrimary else colors.textInverse) else colors.textPrimary
                )

                // Interactive Confirmation Card for Natural Language Extracted Expenses
                message.pendingExpense?.let { pending ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .border(1.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DotMatrixText(
                                    text = "ACTION REQUIRED",
                                    fontSize = 10.sp,
                                    color = colors.accentRed
                                )
                                Text(
                                    text = "Tap to record",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = colors.textMuted
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = pending.merchant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${pending.category} • ${pending.bankName}",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }

                                DotMatrixText(
                                    text = "₹%.2f".format(Locale.ENGLISH, pending.amountPaise / 100.0),
                                    fontSize = 16.sp,
                                    color = colors.textPrimary
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.surfaceDark)
                                        .clickable { onConfirmExpense(pending, message.id) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓ Confirm & Save",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (colors.isDark) colors.textPrimary else colors.textInverse
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                                        .clickable { onDismissExpense(message.id) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Cancel",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
