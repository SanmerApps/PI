package dev.sanmer.pi.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
    modifier = Modifier.height(FilterChipDefaults.Height),
    selected = selected,
    onClick = onClick,
    label = { Text(text = label) },
    shape = CircleShape,
    leadingIcon = when {
        selected -> null
        else -> {
            {
                Dot(
                    modifier = Modifier
                        .padding(5.dp)
                        .size(8.dp),
                    color = LocalContentColor.current
                )
            }
        }
    },
    trailingIcon = when {
        selected -> {
            {
                Icon(
                    painter = painterResource(R.drawable.check_bold),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        }

        else -> null
    }
)