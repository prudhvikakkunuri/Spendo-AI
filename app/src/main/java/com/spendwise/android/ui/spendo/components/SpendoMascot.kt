package com.spendwise.android.ui.spendo.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendwise.android.ui.spendo.theme.SpendoColor

/**
 * Spendo's Original 3D AI Agent Mascot:
 * A sleek, Nothing Phone-inspired cybernetic agent featuring a rounded chassis,
 * tinted visor, glowing matrix eye sensor, and telemetry beacon.
 */
@Composable
fun SpendoMascot(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    isActive: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MascotPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )
    val activeGreen = SpendoColor.SuccessGreen

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // 1. Outer Chassis - Rounded Squircle
        drawRoundRect(
            color = Color(0xFF1E1E1E),
            topLeft = Offset(w * 0.1f, h * 0.12f),
            size = Size(w * 0.8f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
        )

        // Subtle chassis inner border
        drawRoundRect(
            color = Color(0xFF333333),
            topLeft = Offset(w * 0.1f, h * 0.12f),
            size = Size(w * 0.8f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.28f, h * 0.28f),
            style = Stroke(width = w * 0.03f)
        )

        // 2. Visor Screen Glass
        drawRoundRect(
            color = Color(0xFF0D0D0D),
            topLeft = Offset(w * 0.22f, h * 0.26f),
            size = Size(w * 0.56f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.12f, h * 0.12f)
        )

        // 3. Dot-Matrix Eye Arrays (Dual horizontal sensor lights)
        val eyeColor = if (isActive) activeGreen.copy(alpha = pulseAlpha) else Color(0xFF555555)
        val eyeRadius = w * 0.05f

        // Left Eye Matrix Dot
        drawCircle(
            color = eyeColor,
            radius = eyeRadius,
            center = Offset(w * 0.38f, h * 0.44f)
        )
        // Right Eye Matrix Dot
        drawCircle(
            color = eyeColor,
            radius = eyeRadius,
            center = Offset(w * 0.62f, h * 0.44f)
        )

        // Bridge line between eyes (subtle HUD element)
        drawLine(
            color = eyeColor.copy(alpha = 0.4f),
            start = Offset(w * 0.44f, h * 0.44f),
            end = Offset(w * 0.56f, h * 0.44f),
            strokeWidth = w * 0.015f
        )

        // 4. Nothing-inspired Red Beacon Dot (Top Right Antenna)
        drawCircle(
            color = SpendoColor.AccentRed,
            radius = w * 0.045f,
            center = Offset(w * 0.78f, h * 0.18f)
        )
    }
}

/**
 * Mascot Widget Banner with contextual dialogue / financial agent message.
 */
@Composable
fun SpendoMascotWidget(
    message: String,
    modifier: Modifier = Modifier,
    statusText: String = "AI AGENT • ACTIVE",
    onClick: (() -> Unit)? = null
) {
    SpendoCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SpendoMascot(
                size = 52.dp,
                isActive = true
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SpendoColor.SuccessGreen)
                    )
                    Text(
                        text = statusText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SpendoColor.TextSecondary
                    )
                }

                Text(
                    text = message,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = SpendoColor.TextPrimary
                )
            }
        }
    }
}
