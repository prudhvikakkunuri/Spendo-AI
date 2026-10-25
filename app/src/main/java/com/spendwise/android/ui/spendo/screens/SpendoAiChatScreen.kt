package com.spendwise.android.ui.spendo.screens

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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoMascot
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.model.SpendoChatMessage
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoTheme

@Composable
fun SpendoAiChatScreen(
    uiState: SpendoUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onNavigateToAiConfig: () -> Unit,
    onConfirmExpense: (com.spendwise.android.ui.spendo.model.PendingExpenseConfirmation, String) -> Unit = { _, _ -> },
    onDismissExpense: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SpendoTheme.colors
    val listState = rememberLazyListState()

    var isRecordingAudio by remember { mutableStateOf(false) }
    var voiceTranscript by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    val suggestedQuestions = listOf(
        "Spent ₹280 on biryani using HDFC",
        "How much did I spend today?",
        "What is my biggest expense category this month?",
        "How much did I spend on Swiggy?",
        "Compare my spending with last month",
        "How can I reduce my spending next month?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Dedicated Header with Spendo Mascot & Configuration Action
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
                            text = if (uiState.aiConfig.isConfigured) "AI ONLINE • ${uiState.aiConfig.provider.displayName}" else "OFFLINE MODE",
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

        // Live Chat Message Feed
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

        // Voice Assistant Active Recording Bar (Nothing OS dot-matrix style)
        AnimatedVisibility(visible = isRecordingAudio) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
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
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(colors.accentRed)
                            )
                            Text(
                                text = "LISTENING (VOICE AGENT)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentRed
                            )
                        }

                        IconButton(
                            onClick = {
                                isRecordingAudio = false
                                voiceTranscript = null
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    Text(
                        text = voiceTranscript ?: "Speak naturally: 'Add ₹250 for lunch paid through Kotak' or 'What is my total balance?'",
                        fontSize = 13.sp,
                        color = if (voiceTranscript != null) colors.cardDarkText else colors.cardDarkTextSecondary,
                        lineHeight = 18.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SpendoOutlinedButton(
                            text = "Send Voice Command",
                            onClick = {
                                val textToSend = voiceTranscript ?: "Show my total balance across bank accounts"
                                isRecordingAudio = false
                                voiceTranscript = null
                                onSendMessage(textToSend)
                            },
                            modifier = Modifier.weight(1f)
                        )
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

            // Visible Microphone Button
            IconButton(
                onClick = {
                    if (isRecordingAudio) {
                        isRecordingAudio = false
                    } else {
                        isRecordingAudio = true
                        // Pre-populate with a representative voice speech sample for realistic testing
                        voiceTranscript = "Add ₹250 for lunch paid through Kotak"
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isRecordingAudio) colors.accentRed else colors.surfaceVariant)
            ) {
                Icon(
                    imageVector = if (isRecordingAudio) Icons.Outlined.Stop else Icons.Outlined.Mic,
                    contentDescription = "Voice Assistant",
                    tint = if (isRecordingAudio) Color.White else colors.textPrimary,
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
}

@Composable
private fun ChatBubble(
    message: SpendoChatMessage,
    onConfirmExpense: (com.spendwise.android.ui.spendo.model.PendingExpenseConfirmation, String) -> Unit,
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
                                    text = "₹%.2f".format(java.util.Locale.ENGLISH, pending.amountPaise / 100.0),
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
