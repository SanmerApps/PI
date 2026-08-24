package dev.sanmer.pi.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R

@Composable
fun FilterItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) = FilterChip(
    selected = selected,
    onClick = onClick,
    label = { Text(text = label) },
    shape = CircleShape,
    leadingIcon = when {
        selected -> null
        else -> {
            {
                Dot(
                    modifier = Modifier.size(8.dp),
                    color = LocalContentColor.current
                )
            }
        }
    },
    trailingIcon = when {
        selected -> {
            {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null
                )
            }
        }

        else -> null
    }
)