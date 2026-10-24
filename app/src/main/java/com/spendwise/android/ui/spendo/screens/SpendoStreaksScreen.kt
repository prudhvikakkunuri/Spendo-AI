package com.spendwise.android.ui.spendo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.components.DotGridBadge
import com.spendwise.android.ui.spendo.components.DotMatrixText
import com.spendwise.android.ui.spendo.components.SpendoCard
import com.spendwise.android.ui.spendo.components.SpendoDarkCard
import com.spendwise.android.ui.spendo.components.SpendoTopBar
import com.spendwise.android.ui.spendo.model.SpendoUiState
import com.spendwise.android.ui.spendo.theme.SpendoColor

data class AchievementBadge(
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val icon: ImageVector
)

@Composable
fun SpendoStreaksScreen(
    uiState: SpendoUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val badges = listOf(
        AchievementBadge(
            title = "First Step",
            description = "Tracked initial financial expense in Spendo",
            isUnlocked = true,
            icon = Icons.Outlined.Check
        ),
        AchievementBadge(
            title = "Streak Builder",
            description = "Maintained 5 consecutive days within daily ceiling",
            isUnlocked = true,
            icon = Icons.Outlined.LocalFireDepartment
        ),
        AchievementBadge(
            title = "Shield of Savings",
            description = "Kept discretionary weekend spending under budget limit",
            isUnlocked = true,
            icon = Icons.Outlined.Shield
        ),
        AchievementBadge(
            title = "Financial Zenith",
            description = "Surpass 14 days consecutive disciplined streak",
            isUnlocked = false,
            icon = Icons.Outlined.EmojiEvents
        ),
        AchievementBadge(
            title = "Zen Master",
            description = "Complete an entire monthly cycle with 0 impulse spikes",
            isUnlocked = false,
            icon = Icons.Outlined.Spa
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpendoColor.Background)
    ) {
        SpendoTopBar(
            title = "Streaks & Milestones",
            categoryBadge = "DISCIPLINE & HABITS",
            onNavigateBack = onNavigateBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Streak Dark Card
            item {
                SpendoDarkCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DotGridBadge(
                                text = "Current Streak Active",
                                indicatorColor = SpendoColor.SuccessGreen,
                                containerColor = SpendoColor.DarkCardContainer,
                                textColor = SpendoColor.TextInverse
                            )
                            Text(
                                text = "BEST: 14 DAYS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpendoColor.TextInverse
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DotMatrixText(text = "Consecutive Safe Days", fontSize = 11.sp, color = SpendoColor.TextInverseMuted)
                            Text(
                                text = "${uiState.currentStreakDays} DAYS",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                color = SpendoColor.TextInverse
                            )
                        }

                        Text(
                            text = "Every day within your daily ceiling compounds into greater financial peace.",
                            fontSize = 13.sp,
                            color = SpendoColor.TextInverseMuted
                        )

                        // 7-day Weekly Grid Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val streakStatus = listOf(true, true, true, true, true, false, true)
                            days.forEachIndexed { idx, day ->
                                val passed = streakStatus[idx]
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (passed) SpendoColor.SuccessGreen else SpendoColor.DarkTrack
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (passed) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = SpendoColor.SurfaceDark,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = day,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = SpendoColor.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Milestone Badges Header
            item {
                DotMatrixText(text = "Discipline Badges", fontSize = 11.sp, color = SpendoColor.TextSecondary)
            }

            items(badges) { badge ->
                SpendoCard(contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (badge.isUnlocked) SpendoColor.SurfaceDark else SpendoColor.SurfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = badge.icon,
                                contentDescription = null,
                                tint = if (badge.isUnlocked) SpendoColor.SuccessGreen else SpendoColor.TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (badge.isUnlocked) SpendoColor.TextPrimary else SpendoColor.TextSecondary
                            )
                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SpendoColor.TextSecondary
                            )
                        }

                        Text(
                            text = if (badge.isUnlocked) "UNLOCKED" else "LOCKED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (badge.isUnlocked) SpendoColor.SuccessGreen else SpendoColor.TextMuted
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
