package dev.sanmer.pi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ColumnScope.DragHandle() {
    Spacer(
        modifier = Modifier
            .padding(vertical = 20.dp)
            .size(width = 32.dp, height = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.extraLarge
            )
            .align(Alignment.CenterHorizontally)
    )
}