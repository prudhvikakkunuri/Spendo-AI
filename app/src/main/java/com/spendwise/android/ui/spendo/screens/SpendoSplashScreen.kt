package com.spendwise.android.ui.spendo.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Spendo Native Startup Splash Animation
 *
 * Implements the 6-stage Nothing OS-inspired startup sequence:
 * 1. Initial State (120ms): Clean black screen
 * 2. Circle Appears (240ms): Outer ring fades in and scales smoothly (92% -> 100%)
 * 3. Dot Ring Animates (280ms): 32 technical dots sweep around the concentric circle
 * 4. S Mark Draws In (340ms): White geometric S stroke draws into view via path-measure
 * 5. Accent Appears (180ms): Red circular node scales into position at the top right
 * 6. Brand Reveal (240ms): "SPENDO" and "YOUR MONEY. YOUR CONTROL." fade in
 *
 * Total animation duration: ~1.45 seconds.
 */
@Composable
fun SpendoSplashScreen(
    modifier: Modifier = Modifier,
    onSplashComplete: () -> Unit
) {
    // Stage Animatable States
    val outerCircleAlpha = remember { Animatable(0f) }
    val outerCircleScale = remember { Animatable(0.92f) }
    val dotRingProgress = remember { Animatable(0f) }
    val symbolDrawProgress = remember { Animatable(0f) }
    val redAccentScale = remember { Animatable(0f) }
    val redAccentAlpha = remember { Animatable(0f) }
    val brandAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Stage 1: Initial clean black pause
        delay(120)

        // Stage 2: Outer circle appears
        outerCircleAlpha.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
        outerCircleScale.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))

        // Stage 3: Dotted ring animates in (circular sweep)
        dotRingProgress.animateTo(1f, animationSpec = tween(280, easing = LinearOutSlowInEasing))

        // Stage 4: Geometric S mark draws in
        symbolDrawProgress.animateTo(1f, animationSpec = tween(340, easing = FastOutSlowInEasing))

        // Stage 5: Red accent appears
        redAccentAlpha.animateTo(1f, animationSpec = tween(180, easing = FastOutSlowInEasing))
        redAccentScale.animateTo(1f, animationSpec = tween(180, easing = FastOutSlowInEasing))

        // Stage 6: Brand Reveal
        brandAlpha.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))

        // Stage 7: Transition to main app
        delay(260)
        onSplashComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Allow instant skip on tap for testing or rapid access
                onSplashComplete()
            }
            .semantics { contentDescription = "Spendo Startup Splash Screen" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Spendo Logo Canvas
            SpendoAnimatedLogoCanvas(
                size = 180.dp,
                outerCircleAlpha = outerCircleAlpha.value,
                outerCircleScale = outerCircleScale.value,
                dotRingProgress = dotRingProgress.value,
                symbolDrawProgress = symbolDrawProgress.value,
                redAccentScale = redAccentScale.value,
                redAccentAlpha = redAccentAlpha.value
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Stage 6: Wordmark & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
            ) {
                Text(
                    text = "SPENDO",
                    color = Color.White.copy(alpha = brandAlpha.value),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 8.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "YOUR MONEY. YOUR CONTROL.",
                    color = Color(0xFFA0A0A0).copy(alpha = brandAlpha.value),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.5.sp
                )
            }
        }
    }
}

/**
 * Precision Canvas rendering the multi-stage animated Spendo Logo.
 */
@Composable
private fun SpendoAnimatedLogoCanvas(
    size: Dp,
    outerCircleAlpha: Float,
    outerCircleScale: Float,
    dotRingProgress: Float,
    symbolDrawProgress: Float,
    redAccentScale: Float,
    redAccentAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val maxR = (w.coerceAtMost(h) / 2f)

        // 1. Stage 2: Outer Solid Circle Ring
        if (outerCircleAlpha > 0f) {
            val outerRadius = maxR * 0.90f
            scale(outerCircleScale, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = Color.White.copy(alpha = outerCircleAlpha * 0.95f),
                    radius = outerRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }

        // 2. Stage 3: Concentric Dotted Ring (32 dots revealed around circle)
        if (dotRingProgress > 0f) {
            val dotRadius = maxR * 0.72f
            val tickCount = 32
            val visibleCount = (tickCount * dotRingProgress).toInt().coerceAtMost(tickCount)

            for (i in 0 until visibleCount) {
                val angleRad = (i * (360.0 / tickCount) - 90.0) * (Math.PI / 180.0)
                val dx = cx + (dotRadius * cos(angleRad)).toFloat()
                val dy = cy + (dotRadius * sin(angleRad)).toFloat()

                // Dots fade in smoothly as progress reaches them
                val dotAlpha = ((dotRingProgress * tickCount) - i).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(0xFFA0A0A0).copy(alpha = dotAlpha),
                    radius = 1.4.dp.toPx(),
                    center = Offset(dx, dy)
                )
            }
        }

        // 3. Stage 4: Geometric S Monogram Drawing
        if (symbolDrawProgress > 0f) {
            val strokeW = 4.2.dp.toPx()

            // Define exact Spendo Geometric S path
            // Bottom-left node -> horizontal right -> 180 arc up -> horizontal left -> 180 arc up -> horizontal right
            val sPath = Path().apply {
                val startX = cx - (w * 0.16f)
                val startY = cy + (h * 0.18f)
                moveTo(startX, startY)

                val bottomEndX = cx + (w * 0.08f)
                lineTo(bottomEndX, startY)

                // Lower loop arc up & left
                val midY = cy
                val midLeftX = cx - (w * 0.08f)
                cubicTo(
                    cx + (w * 0.22f), startY,
                    cx + (w * 0.22f), midY,
                    cx, midY
                )
                lineTo(midLeftX, midY)

                // Upper loop arc up & right
                val topY = cy - (h * 0.18f)
                val topEndX = cx + (w * 0.12f)
                cubicTo(
                    cx - (w * 0.22f), midY,
                    cx - (w * 0.22f), topY,
                    cx, topY
                )
                lineTo(topEndX, topY)
            }

            // PathMeasure to draw only the percentage completed
            val pathMeasure = PathMeasure()
            pathMeasure.setPath(sPath, false)
            val pathLength = pathMeasure.length

            val dstPath = Path()
            pathMeasure.getSegment(0f, pathLength * symbolDrawProgress, dstPath, true)

            drawPath(
                path = dstPath,
                color = Color.White,
                style = Stroke(
                    width = strokeW,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Lower-Left Connected White Circular Node
            val lowerNodeCenter = Offset(cx - (w * 0.16f), cy + (h * 0.18f))
            val lowerNodeRadius = (3.6.dp.toPx() * symbolDrawProgress.coerceAtMost(1f))
            drawCircle(
                color = Color.White,
                radius = lowerNodeRadius,
                center = lowerNodeCenter
            )
        }

        // 4. Stage 5: Upper-Right Accent Red Ring Node
        if (redAccentAlpha > 0f) {
            val redNodeCenter = Offset(cx + (w * 0.20f), cy - (h * 0.22f))
            val baseRadius = 4.4.dp.toPx() * redAccentScale

            // Draw outer red ring with transparent/black center
            drawCircle(
                color = Color(0xFFE60023).copy(alpha = redAccentAlpha),
                radius = baseRadius,
                center = redNodeCenter,
                style = Stroke(width = 2.2.dp.toPx())
            )
        }
    }
}
