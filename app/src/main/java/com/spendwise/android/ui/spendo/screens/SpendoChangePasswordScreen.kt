package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.domain.security.PasswordChangeResult
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoPasswordField
import com.spendwise.android.ui.spendo.components.SpendoPrimaryButton
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.theme.SpendoColor
import kotlinx.coroutines.launch

@Composable
fun SpendoChangePasswordScreen(
    hasCustomPassword: Boolean,
    onChangePassword: suspend (current: String, newPass: String) -> PasswordChangeResult,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var currentPasswordError by remember { mutableStateOf<String?>(null) }
    var newPasswordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Change Password",
            categoryBadge = "SECURITY",
            onNavigateBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Context Header Card
            SpendoDarkCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DotMatrixText(
                        text = "Credential Management",
                        fontSize = 11.sp,
                        color = SpendoColor.CardDarkTextSecondary
                    )
                    Text(
                        text = if (hasCustomPassword) "Update Security Password" else "Set Custom Security Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.CardDarkText
                    )
                    Text(
                        text = if (hasCustomPassword) {
                            "Your security password protects concealed bank balances and prevents unauthorized balance reveals."
                        } else {
                            "You are currently using the default security PIN (4892). Replace it with a personal password."
                        },
                        fontSize = 12.sp,
                        color = SpendoColor.CardDarkTextSecondary
                    )
                }
            }

            // Input Fields Card
            SpendoCard(contentPadding = 20.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // 1. Current Password
                    SpendoPasswordField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            currentPasswordError = null
                        },
                        label = "CURRENT PASSWORD",
                        placeholder = if (hasCustomPassword) "Enter existing password" else "Enter default PIN (4892)",
                        isError = currentPasswordError != null,
                        errorMessage = currentPasswordError
                    )

                    // 2. New Password
                    SpendoPasswordField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            newPasswordError = null
                        },
                        label = "NEW PASSWORD",
                        placeholder = "Minimum 4 characters",
                        isError = newPasswordError != null,
                        errorMessage = newPasswordError
                    )

                    // 3. Confirm New Password
                    SpendoPasswordField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = null
                        },
                        label = "CONFIRM NEW PASSWORD",
                        placeholder = "Re-enter new password",
                        isError = confirmPasswordError != null,
                        errorMessage = confirmPasswordError
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save Password Action
                    SpendoPrimaryButton(
                        text = if (isSaving) "Verifying & Saving..." else "Save Password",
                        enabled = !isSaving,
                        onClick = {
                            var hasError = false

                            if (currentPassword.isEmpty()) {
                                currentPasswordError = "Current password is required"
                                hasError = true
                            }

                            if (newPassword.length < 4) {
                                newPasswordError = "New password must be at least 4 characters"
                                hasError = true
                            }

                            if (newPassword != confirmPassword) {
                                confirmPasswordError = "Passwords do not match"
                                hasError = true
                            }

                            if (newPassword == currentPassword && currentPassword.isNotEmpty()) {
                                newPasswordError = "New password must be different from current password"
                                hasError = true
                            }

                            if (!hasError) {
                                isSaving = true
                                coroutineScope.launch {
                                    val result = onChangePassword(currentPassword, newPassword)
                                    isSaving = false
                                    when (result) {
                                        is PasswordChangeResult.Success -> {
                                            successDialogMessage = "Password successfully changed! Your new password is now active."
                                            currentPassword = ""
                                            newPassword = ""
                                            confirmPassword = ""
                                        }
                                        is PasswordChangeResult.InvalidCurrentPassword -> {
                                            currentPasswordError = "Current password is incorrect"
                                        }
                                        is PasswordChangeResult.PasswordTooShort -> {
                                            newPasswordError = "Password must be at least 4 characters"
                                        }
                                        is PasswordChangeResult.SameAsCurrentPassword -> {
                                            newPasswordError = "New password cannot be the same as current"
                                        }
                                        is PasswordChangeResult.Error -> {
                                            currentPasswordError = result.message
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (successDialogMessage != null) {
        AlertDialog(
            onDismissRequest = {
                successDialogMessage = null
                onNavigateBack()
            },
            title = {
                Text(
                    text = "PASSWORD UPDATED",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SpendoColor.TextPrimary
                )
            },
            text = {
                Text(
                    text = successDialogMessage ?: "",
                    fontSize = 13.sp,
                    color = SpendoColor.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        successDialogMessage = null
                        onNavigateBack()
                    }
                ) {
                    Text(
                        text = "DONE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SpendoColor.TextPrimary
                    )
                }
            },
            containerColor = SpendoColor.Surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
