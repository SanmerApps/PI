package dev.sanmer.pi.ui.screens.install.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun TittleItem(
    text: String,
) = Text(
    text = text,
    style = MaterialTheme.typography.titleMedium
)