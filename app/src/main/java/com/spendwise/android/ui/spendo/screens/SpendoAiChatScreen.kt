package com.spendwise.android.ui.spendo.screens

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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoAiChatScreen(
    uiState: SpendoUiState,
    onInputChange: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onNavigateToAiConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.chatMessages.size) {
        if (uiState.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatMessages.size - 1)
        }
    }

    val suggestedQuestions = listOf(
        "What did I spend today?",
        "Show my expenses for 22 October.",
        "Where did most of my money go?",
        "How much budget do I have left?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        // Dedicated Header with Spendo Mascot & Configuration Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpendoColor.Background)
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
                                .background(if (uiState.aiConfig.isConfigured) SpendoColor.SuccessGreen else SpendoColor.AccentRed)
                        )
                        DotMatrixText(
                            text = if (uiState.aiConfig.isConfigured) "AI ONLINE • ${uiState.aiConfig.provider.displayName}" else "NOT CONFIGURED",
                            fontSize = 10.sp,
                            color = SpendoColor.TextSecondary
                        )
                    }

                    Text(
                        text = "Spendo AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.TextPrimary
                    )
                }
            }

            // Visible Configure AI Button
            SpendoOutlinedButton(
                text = "Configure",
                onClick = onNavigateToAiConfig,
                height = 36.dp,
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                }
            )
        }

        // Unconfigured Gate Banner (if provider not configured yet)
        if (!uiState.aiConfig.isConfigured) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                SpendoCard(
                    backgroundColor = SpendoColor.SurfaceDark,
                    borderColor = SpendoColor.BorderDark,
                    contentPadding = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SpendoColor.AccentRedSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = SpendoColor.AccentRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Provider Not Configured",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextInverse
                            )
                            Text(
                                text = "Bring your own API key (Groq, Cerebras, Gemini, OpenAI) to unlock intelligence.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = SpendoColor.TextSecondary
                            )
                        }

                        Text(
                            text = "SET UP →",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.AccentRed,
                            modifier = Modifier.clickable { onNavigateToAiConfig() }
                        )
                    }
                }
            }
        }

        // Chat Message Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.chatMessages, key = { it.id }) { msg ->
                ChatBubble(message = msg)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Suggested Prompt Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedQuestions.forEach { question ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(SpendoColor.Surface)
                        .border(1.dp, SpendoColor.Divider, RoundedCornerShape(18.dp))
                        .clickable { onSendMessage(question) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = question,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SpendoColor.TextPrimary
                    )
                }
            }
        }

        // Message Composer with Send Button & placeholder support for expense entry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpendoColor.Surface)
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
                        text = "Ask Spendo or type: 'Spent 400 on lunch'...",
                        fontSize = 13.sp,
                        color = SpendoColor.TextMuted
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SpendoColor.Background,
                    unfocusedContainerColor = SpendoColor.Background,
                    focusedIndicatorColor = SpendoColor.TextPrimary,
                    unfocusedIndicatorColor = SpendoColor.Divider
                )
            )

            IconButton(
                onClick = { onSendMessage(null) },
                enabled = uiState.chatInputText.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (uiState.chatInputText.isNotBlank()) SpendoColor.SurfaceDark else SpendoColor.SurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (uiState.chatInputText.isNotBlank()) SpendoColor.TextInverse else SpendoColor.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: SpendoChatMessage) {
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

            Box(
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
                    .background(if (isUser) SpendoColor.SurfaceDark else SpendoColor.Surface)
                    .border(
                        1.dp,
                        if (isUser) SpendoColor.BorderDark else SpendoColor.Divider,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isUser) SpendoColor.TextInverse else SpendoColor.TextPrimary
                )
            }
        }
    }
}
