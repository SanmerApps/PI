package dev.sanmer.pi.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

@Composable
fun Logo(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    fraction: Float = 0.6f
) = Surface(
    modifier = modifier,
    shape = shape,
    color = containerColor,
    contentColor = contentColor
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.fillMaxSize(fraction),
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = LocalContentColor.current
        )
    }
}

@Composable
fun Logo(
    text: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
    )
) = Surface(
    modifier = modifier,
    shape = shape,
    color = containerColor,
    contentColor = contentColor
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        val label by remember(text) {
            derivedStateOf {
                if (text.isNotEmpty()) {
                    text.first().toString()
                } else {
                    "?"
                }
            }
        }

        Text(
            text = label,
            color = contentColor,
            style = textStyle
        )
    }
}