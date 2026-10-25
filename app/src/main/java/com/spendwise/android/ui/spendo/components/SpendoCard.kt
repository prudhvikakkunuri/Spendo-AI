package com.spendwise.android.ui.spendo.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spendwise.android.ui.spendo.theme.SpendoColor

/**
 * Standard Nothing OS-inspired Card:
 * Clean white or dark surface with 1px hairline border and 22.dp rounded squircle corners.
 */
@Composable
fun SpendoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = SpendoColor.Surface,
    borderColor: Color = SpendoColor.BorderSubtle,
    contentPadding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content
    )
}

/**
 * Inverted Dark Section Card for Hero cards, telemetry headers, and dark cards.
 */
@Composable
fun SpendoDarkCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    contentPadding: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    SpendoCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = SpendoColor.CardDark,
        borderColor = SpendoColor.CardDarkBorder,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content
    )
}
