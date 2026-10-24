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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoEmptyState
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoNotificationItem
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor

@Composable
fun SpendoNotificationScreen(
    uiState: SpendoUiState,
    onCategorySelected: (String) -> Unit,
    onClearAll: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredNotifications = if (uiState.selectedNotificationCategory == "All") {
        uiState.notifications
    } else {
        uiState.notifications.filter { it.category == uiState.selectedNotificationCategory }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Notification Centre",
            categoryBadge = "ALERTS",
            onNavigateBack = onNavigateBack,
            actions = {
                if (uiState.notifications.isNotEmpty()) {
                    IconButton(onClick = onClearAll) {
                        Icon(
                            imageVector = Icons.Outlined.ClearAll,
                            contentDescription = "Clear All",
                            tint = SpendoColor.TextSecondary
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Transactions", "Budgets", "Motivation").forEach { category ->
                    val isSelected = uiState.selectedNotificationCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategorySelected(category) },
                        label = { Text(category, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, SpendoColor.Divider),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpendoColor.SurfaceDark,
                            selectedLabelColor = SpendoColor.TextInverse,
                            containerColor = SpendoColor.Surface,
                            labelColor = SpendoColor.TextSecondary
                        )
                    )
                }
            }

            if (filteredNotifications.isEmpty()) {
                SpendoEmptyState(
                    title = "All Caught Up",
                    message = "No unread alerts or financial notifications in this category.",
                    icon = Icons.Outlined.Notifications
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { notif ->
                        NotificationCard(item = notif)
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: SpendoNotificationItem,
    modifier: Modifier = Modifier
) {
    SpendoCard(
        modifier = modifier,
        contentPadding = 16.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when (item.category) {
                                    "Transactions" -> SpendoColor.TextPrimary
                                    "Budgets" -> SpendoColor.AccentRed
                                    else -> SpendoColor.SuccessGreen
                                }
                            )
                    )
                    DotMatrixText(
                        text = item.category,
                        fontSize = 10.sp,
                        color = SpendoColor.TextSecondary
                    )
                }

                Text(
                    text = item.timestampText,
                    fontSize = 11.sp,
                    color = SpendoColor.TextMuted
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SpendoColor.TextPrimary
            )

            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium,
                color = SpendoColor.TextSecondary
            )

            if (item.progress != null) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { item.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (item.progress >= 1.0f) SpendoColor.AccentRed else SpendoColor.SuccessGreen,
                        trackColor = SpendoColor.SurfaceVariant
                    )
                }
            }
        }
    }
}
