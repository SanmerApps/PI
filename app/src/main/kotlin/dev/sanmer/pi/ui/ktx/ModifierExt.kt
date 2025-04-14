package dev.sanmer.pi.ui.ktx

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

@Stable
fun Modifier.surface(
    shape: Shape,
    backgroundColor: Color,
    border: BorderStroke? = null
) = then(if (border != null) Modifier.border(border = border, shape = shape) else Modifier)
    .background(color = backgroundColor, shape = shape)
    .clip(shape = shape)