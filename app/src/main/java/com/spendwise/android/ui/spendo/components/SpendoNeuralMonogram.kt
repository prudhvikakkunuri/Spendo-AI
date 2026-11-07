package com.spendwise.android.ui.spendo.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unique Spendo Neural Monogram:
 * A technical, Nothing OS-inspired geometric 'S' integrated with neural nodes:
 * - Upper terminal: Distinct red circular neural ring/node
 * - Lower terminal: Clean white circular connected node
 * - Smooth geometric S-line connecting them
 * - Optional surrounding technical glyph tick marks
 */
@Composable
fun SpendoNeuralMonogram(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showGlyphRing: Boolean = false,
    ringColor: Color = Color(0xFF333333),
    monogramColor: Color = Color.White,
    accentRed: Color = Color(0xFFFF2E2E)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f

        // Draw surrounding technical glyph tick marks (circular dial)
        if (showGlyphRing) {
            val ringRadius = (w.coerceAtMost(h) / 2f) - 2.dp.toPx()
            val tickCount = 32
            for (i in 0 until tickCount) {
                val angleRad = (i * (360.0 / tickCount)) * (Math.PI / 180.0)
                val dotRadius = ringRadius
                val dx = cx + (dotRadius * cos(angleRad)).toFloat()
                val dy = cy + (dotRadius * sin(angleRad)).toFloat()
                drawCircle(
                    color = if (i % 8 == 0) Color(0xFF666666) else ringColor,
                    radius = if (i % 8 == 0) 1.2.dp.toPx() else 0.8.dp.toPx(),
                    center = Offset(dx, dy)
                )
            }
        }

        // Geometric S path
        val strokeWidth = 2.4.dp.toPx()
        val path = Path().apply {
            // Start at bottom node
            val startX = w * 0.38f
            val startY = h * 0.68f
            moveTo(startX, startY)

            // Bottom horizontal line right
            val btmRightX = w * 0.62f
            val btmRightY = h * 0.68f
            lineTo(btmRightX, btmRightY)

            // Curve up-left through center to top-left
            cubicTo(
                w * 0.74f, h * 0.68f,
                w * 0.74f, h * 0.50f,
                w * 0.50f, h * 0.50f
            )
            cubicTo(
                w * 0.26f, h * 0.50f,
                w * 0.26f, h * 0.32f,
                w * 0.38f, h * 0.32f
            )

            // Top horizontal line right
            val topRightX = w * 0.60f
            val topRightY = h * 0.32f
            lineTo(topRightX, topRightY)
        }

        // Draw white S-curve
        drawPath(
            path = path,
            color = monogramColor,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Bottom white neural node
        drawCircle(
            color = monogramColor,
            radius = 2.8.dp.toPx(),
            center = Offset(w * 0.35f, h * 0.68f)
        )

        // Upper red neural node ring (at top right)
        drawCircle(
            color = accentRed,
            radius = 3.6.dp.toPx(),
            center = Offset(w * 0.68f, h * 0.25f),
            style = Stroke(width = 1.8.dp.toPx())
        )
    }
}
