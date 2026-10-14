package dev.sanmer.pi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun Logo(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) = Box(
    modifier = modifier
        .background(
            color = containerColor,
            shape = CircleShape
        ),
    contentAlignment = Alignment.Center
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = Modifier.fillMaxWidth(0.6f),
        tint = contentColor
    )
}