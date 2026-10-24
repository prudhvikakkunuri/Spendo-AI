package com.spendwise.android.ui.spendo.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.theme.SpendoColor

/**
 * Dot-Matrix styled text for uppercase headings, telemetry metrics, and category badges.
 * Gives the distinctive Nothing OS technological feel without compromising body readability.
 */
@Composable
fun DotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp,
    color: Color = SpendoColor.TextSecondary,
    fontWeight: FontWeight = FontWeight.Bold,
    letterSpacing: TextUnit = 1.4.sp
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontFamily = FontFamily.Monospace,
        fontWeight = fontWeight,
        fontSize = fontSize,
        letterSpacing = letterSpacing,
        color = color
    )
}

/**
 * Large financial number display with dot-matrix tracking and bold weight.
 */
@Composable
fun DotMatrixAmount(
    amountText: String,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
    fontSize: TextUnit = 32.sp,
    color: Color = SpendoColor.TextPrimary,
    showRedDot: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = currencySymbol,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = (fontSize.value * 0.72f).sp,
            color = color.copy(alpha = 0.8f)
        )
        Text(
            text = amountText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            letterSpacing = 1.sp,
            color = color
        )
        if (showRedDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(SpendoColor.AccentRed)
            )
        }
    }
}

/**
 * An ornamental dot-grid pill badge for statuses, labels, and streak indicators.
 */
@Composable
fun DotGridBadge(
    text: String,
    modifier: Modifier = Modifier,
    indicatorColor: Color = SpendoColor.AccentRed,
    containerColor: Color = SpendoColor.Surface,
    textColor: Color = SpendoColor.TextPrimary
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .border(1.dp, SpendoColor.Divider, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Text(
            text = text.uppercase(),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            color = textColor
        )
    }
}

/**
 * A decorative 4x4 or 3x3 dot-matrix micro pattern canvas for card corners.
 */
@Composable
fun DotMatrixPattern(
    rows: Int = 3,
    cols: Int = 3,
    dotRadius: Dp = 1.5.dp,
    spacing: Dp = 6.dp,
    color: Color = SpendoColor.Divider,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(
            width = (cols * spacing.value).dp,
            height = (rows * spacing.value).dp
        )
    ) {
        val radiusPx = dotRadius.toPx()
        val spacingPx = spacing.toPx()
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                drawCircle(
                    color = color,
                    radius = radiusPx,
                    center = Offset(
                        x = c * spacingPx + radiusPx,
                        y = r * spacingPx + radiusPx
                    )
                )
            }
        }
    }
}
