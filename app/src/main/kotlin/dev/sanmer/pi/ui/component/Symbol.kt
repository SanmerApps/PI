package dev.sanmer.pi.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun Dot(
    modifier: Modifier,
    color: Color
) = Canvas(modifier = modifier) {
    val radius = size.minDimension / 2f

    drawCircle(
        color = color,
        radius = radius,
        center = center
    )
}