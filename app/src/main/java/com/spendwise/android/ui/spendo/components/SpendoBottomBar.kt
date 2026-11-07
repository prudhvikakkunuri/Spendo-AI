package com.spendwise.android.ui.spendo.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.navigation.SpendoScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Legacy bottom nav items retained for backward compatibility.
 */
enum class SpendoBottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
) {
    HOME("Home", SpendoScreen.Home.route, Icons.Outlined.Home),
    ACTIVITY("Activity", SpendoScreen.Activity.route, Icons.AutoMirrored.Outlined.ReceiptLong),
    CALENDAR("Calendar", SpendoScreen.Calendar.route, Icons.Outlined.CalendarMonth),
    SPENDO_AI("Spendo AI", SpendoScreen.SpendoAi.route, Icons.Outlined.AutoAwesome)
}

enum class SpendoMainDestination(
    val title: String,
    val pageIndex: Int,
    val route: String
) {
    HOME("Home", 0, SpendoScreen.Home.route),
    WALLET("Wallet", 1, SpendoScreen.Activity.route),
    ANALYTICS("Analytics", 2, SpendoScreen.Analytics.route),
    BUDGETS("Budgets", 3, SpendoScreen.Budget.route),
    PROFILE("Profile", 4, SpendoScreen.Settings.route);

    companion object {
        fun fromIndex(index: Int): SpendoMainDestination =
            values().firstOrNull { it.pageIndex == index } ?: HOME

        fun fromRoute(route: String): SpendoMainDestination =
            values().firstOrNull { it.route == route } ?: HOME
    }
}

/**
 * Redesigned Spendo Bottom Navigation Bar:
 * - 5 horizontal destinations in a rounded dark pill container (Home, Wallet, Analytics, Budgets, Profile)
 * - Three-dot technical indicator above each tab
 * - Restrained red accents (Home active indicator, Budgets partial red gauge)
 * - Subtle vertical divider
 * - Separate circular Spendo AI button with original Spendo Neural Monogram
 * - Pure monochrome foundation: black, dark gray, white, and signal red
 */
@Composable
fun SpendoBottomBar(
    selectedPageIndex: Int,
    onSelectPage: (Int) -> Unit,
    onSpendoAiClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main 5-Destination Navigation Pill Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(27.dp))
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpendoMainDestination.values().forEach { destination ->
                        val isSelected = destination.pageIndex == selectedPageIndex
                        SpendoNavTab(
                            destination = destination,
                            isSelected = isSelected,
                            onClick = { onSelectPage(destination.pageIndex) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Subtle Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0xFF262626))
            )

            // Separate Circular Spendo AI Button (Height-matched to pill container: 54dp)
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121212))
                    .border(1.dp, Color(0xFF2A2A2A), CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onSpendoAiClick
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Spendo AI Chat"
                    },
                contentAlignment = Alignment.Center
            ) {
                SpendoNeuralMonogram(
                    size = 32.dp,
                    showGlyphRing = true,
                    ringColor = Color(0xFF383838),
                    monogramColor = Color.White,
                    accentRed = Color(0xFFE60023)
                )
            }
        }
    }
}

/**
 * Backward compatible overload accepting currentRoute and onNavigateToDestination.
 */
@Composable
fun SpendoBottomBar(
    currentRoute: String,
    onNavigateToDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = when (currentRoute) {
        SpendoScreen.Home.route -> 0
        SpendoScreen.Activity.route -> 1
        SpendoScreen.Analytics.route -> 2
        SpendoScreen.Budget.route -> 3
        SpendoScreen.Settings.route -> 4
        else -> 0
    }

    SpendoBottomBar(
        selectedPageIndex = selectedIndex,
        onSelectPage = { page ->
            val targetRoute = SpendoMainDestination.fromIndex(page).route
            onNavigateToDestination(targetRoute)
        },
        onSpendoAiClick = {
            onNavigateToDestination(SpendoScreen.SpendoAi.route)
        },
        modifier = modifier
    )
}

/**
 * Individual Destination Tab inside the main pill navigation bar.
 */
@Composable
private fun SpendoNavTab(
    destination: SpendoMainDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tintColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF6E6E6E),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "tab_tint"
    )

    val backgroundModifier = if (isSelected) {
        Modifier
            .clip(RoundedCornerShape(23.dp))
            .background(Color(0xFF222222))
            .border(1.dp, Color(0xFF383838), RoundedCornerShape(23.dp))
    } else {
        Modifier.clip(RoundedCornerShape(23.dp))
    }

    Column(
        modifier = modifier
            .height(46.dp)
            .then(backgroundModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .semantics {
                role = Role.Tab
                contentDescription = destination.title
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Top 3-dot technical glyph
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { dotIndex ->
                val dotColor = if (isSelected) {
                    if (dotIndex == 1) {
                        if (destination == SpendoMainDestination.HOME) Color(0xFFE60023) else Color.White
                    } else {
                        Color(0xFF7A7A7A)
                    }
                } else {
                    Color(0xFF242424)
                }
                Box(
                    modifier = Modifier
                        .size(2.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Destination Icon (Only icon, no text label)
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center
        ) {
            when (destination) {
                SpendoMainDestination.HOME -> HomeNavIcon(tint = tintColor, isSelected = isSelected)
                SpendoMainDestination.WALLET -> WalletNavIcon(tint = tintColor)
                SpendoMainDestination.ANALYTICS -> AnalyticsNavIcon(tint = tintColor)
                SpendoMainDestination.BUDGETS -> BudgetsNavIcon(tint = tintColor)
                SpendoMainDestination.PROFILE -> ProfileNavIcon(tint = tintColor)
            }
        }
    }
}

// -------------------------------------------------------------
// Pixel-Perfect Vector Icons Matching Nothing OS Reference Design
// -------------------------------------------------------------

@Composable
private fun HomeNavIcon(
    tint: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.8.dp.toPx()

        val housePath = Path().apply {
            // Roof peak
            moveTo(w * 0.50f, h * 0.15f)
            // Roof right
            lineTo(w * 0.88f, h * 0.48f)
            // Wall right down
            lineTo(w * 0.88f, h * 0.88f)
            // Floor left
            lineTo(w * 0.12f, h * 0.88f)
            // Wall left up
            lineTo(w * 0.12f, h * 0.48f)
            close()
        }

        drawPath(
            path = housePath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Doorway
        val doorPath = Path().apply {
            moveTo(w * 0.38f, h * 0.88f)
            lineTo(w * 0.38f, h * 0.56f)
            lineTo(w * 0.62f, h * 0.56f)
            lineTo(w * 0.62f, h * 0.88f)
        }
        drawPath(
            path = doorPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Restrained signature red accent dot near top-right of house when selected
        if (isSelected) {
            drawCircle(
                color = Color(0xFFFF2E2E),
                radius = 2.2.dp.toPx(),
                center = Offset(w * 0.82f, h * 0.22f)
            )
        }
    }
}

@Composable
private fun WalletNavIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.8.dp.toPx()

        // Main wallet body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.10f, h * 0.22f),
            size = Size(w * 0.80f, h * 0.62f),
            cornerRadius = CornerRadius(3.5.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // Wallet clasp flap
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.56f, h * 0.40f),
            size = Size(w * 0.34f, h * 0.26f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // Center clasp dot
        drawCircle(
            color = tint,
            radius = 1.2.dp.toPx(),
            center = Offset(w * 0.67f, h * 0.53f)
        )
    }
}

@Composable
private fun AnalyticsNavIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = 2.4.dp.toPx()
        val cornerRadius = CornerRadius(1.2.dp.toPx())

        // Short bar (left)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.18f, h * 0.55f),
            size = Size(barWidth, h * 0.35f),
            cornerRadius = cornerRadius
        )

        // Medium bar (center)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.46f, h * 0.32f),
            size = Size(barWidth, h * 0.58f),
            cornerRadius = cornerRadius
        )

        // Tall bar (right)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.74f, h * 0.12f),
            size = Size(barWidth, h * 0.78f),
            cornerRadius = cornerRadius
        )
    }
}

@Composable
private fun BudgetsNavIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.8.dp.toPx()
        val radius = (w.coerceAtMost(h) / 2f) - (strokeWidth / 2f)
        val center = Offset(w / 2f, h / 2f)

        // 270-degree main circle arc
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Interior pie lines
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x + radius, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = center,
            end = Offset(center.x, center.y - radius),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Top-right 90-degree quadrant arc highlighted in restrained RED
        drawArc(
            color = Color(0xFFFF2E2E),
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ProfileNavIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.8.dp.toPx()

        // Head outline
        drawCircle(
            color = tint,
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.30f),
            style = Stroke(width = strokeWidth)
        )

        // Shoulders arc
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.52f),
            size = Size(w * 0.76f, h * 0.44f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
