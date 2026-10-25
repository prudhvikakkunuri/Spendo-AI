package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoOutlinedButton
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.AiConfigState
import com.spendwise.android.ui.spendo.model.AiProvider
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoAiConfigScreen(
    aiConfig: AiConfigState,
    onSelectProvider: (AiProvider) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSaveConfig: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "AI Configuration",
            categoryBadge = "INTELLIGENCE SETUP",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            SpendoDarkCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DotMatrixText(text = "Agent Status", fontSize = 11.sp, color = SpendoColor.CardDarkTextSecondary)
                        Text(
                            text = if (aiConfig.isConfigured) "Configured & Active" else "Setup Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SpendoColor.CardDarkText
                        )
                        Text(
                            text = if (aiConfig.isConfigured) "Provider: ${aiConfig.provider.displayName}" else "Select your LLM engine below.",
                            fontSize = 12.sp,
                            color = SpendoColor.CardDarkTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (aiConfig.isConfigured) SpendoColor.SuccessGreen else SpendoColor.AccentRed)
                    )
                }
            }

            // Provider Selection
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DotMatrixText(text = "Choose Provider", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiProvider.values().forEach { provider ->
                            val isSelected = aiConfig.provider == provider
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectProvider(provider) },
                                label = {
                                    Text(
                                        text = provider.displayName,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, if (isSelected) SpendoColor.TextPrimary else SpendoColor.Divider),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpendoColor.TextPrimary,
                                    selectedLabelColor = SpendoColor.Background,
                                    containerColor = SpendoColor.Surface,
                                    labelColor = SpendoColor.TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Credentials Card
            SpendoCard(contentPadding = 18.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    DotMatrixText(text = "Credentials & Model", fontSize = 11.sp, color = SpendoColor.TextSecondary)

                    OutlinedTextField(
                        value = aiConfig.apiKey,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("${aiConfig.provider.displayName} API Key") },
                        placeholder = { Text("e.g. gsk_... or sk-...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.Key, contentDescription = null, tint = SpendoColor.TextSecondary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide key" else "Show key",
                                    tint = SpendoColor.TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SpendoColor.Surface,
                            unfocusedContainerColor = SpendoColor.Surface,
                            focusedIndicatorColor = SpendoColor.TextPrimary,
                            unfocusedIndicatorColor = SpendoColor.Divider
                        )
                    )

                    OutlinedTextField(
                        value = aiConfig.model,
                        onValueChange = onModelChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Target Model") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Outlined.SmartToy, contentDescription = null, tint = SpendoColor.TextSecondary)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SpendoColor.Surface,
                            unfocusedContainerColor = SpendoColor.Surface,
                            focusedIndicatorColor = SpendoColor.TextPrimary,
                            unfocusedIndicatorColor = SpendoColor.Divider
                        )
                    )

                    // Test Connection Feedback
                    if (aiConfig.testConnectionMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SpendoColor.SuccessGreenSubtle)
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = aiConfig.testConnectionMessage,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                            )
                        }
                    }

                    // Test Connection Button
                    SpendoOutlinedButton(
                        text = if (aiConfig.isTestingConnection) "Testing Ping..." else "Test Connection",
                        onClick = onTestConnection,
                        enabled = !aiConfig.isTestingConnection && aiConfig.apiKey.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            if (aiConfig.isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = SpendoColor.TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )
                }
            }

            // Local Privacy Guarantee
            SpendoCard(contentPadding = 14.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = SpendoColor.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Your API key is stored securely in encrypted local storage. Spendo never transmits keys to any third-party telemetry or Spendo servers.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = SpendoColor.TextSecondary
                    )
                }
            }

            // Save Configuration Action
            SpendoPrimaryButton(
                text = "Save Configuration",
                onClick = {
                    onSaveConfig()
                    onNavigateBack()
                },
                enabled = aiConfig.apiKey.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
