package com.spendwise.android.ui.spendo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.spendwise.android.ui.spendo.theme.SpendoColor

enum class SpendoBottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
) {
    HOME("Home", "spendo_home", Icons.Outlined.Home),
    ACTIVITY("Activity", "spendo_activity", Icons.AutoMirrored.Outlined.ReceiptLong),
    CALENDAR("Calendar", "spendo_calendar", Icons.Outlined.CalendarMonth),
    SPENDO_AI("Spendo AI", "spendo_ai_chat", Icons.Outlined.AutoAwesome)
}

/**
 * Nothing OS-inspired Bottom Navigation Bar:
 * Monochrome icons with restrained red accent dot indicator on the active destination.
 */
@Composable
fun SpendoBottomBar(
    currentRoute: String,
    onNavigateToDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SpendoColor.Surface)
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = SpendoColor.Divider
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpendoBottomNavItem.values().forEach { item ->
                val isSelected = currentRoute == item.route

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { onNavigateToDestination(item.route) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) SpendoColor.TextPrimary else SpendoColor.TextMuted,
                        modifier = Modifier.size(22.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.title.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp,
                            color = if (isSelected) SpendoColor.TextPrimary else SpendoColor.TextMuted
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(SpendoColor.AccentRed)
                            )
                        }
                    }
                }
            }
        }
    }
}
