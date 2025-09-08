package dev.sanmer.pi.ui.screens.install.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.parser.SplitConfig

@Composable
fun SplitConfigItem(
    config: SplitConfig,
    isRequiredConfig: (SplitConfig) -> Boolean,
    toggleSplitConfig: (SplitConfig) -> Unit,
) {
    val required by remember {
        derivedStateOf { isRequiredConfig(config) }
    }

    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        onClick = { toggleSplitConfig(config) },
        enabled = !config.isDisabled
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIcon(
                config = config,
                enable = required
            )

            Column(
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        !required -> MaterialTheme.colorScheme.outline
                        else -> Color.Unspecified
                    }
                )

                Text(
                    text = buildString {
                        if (config.configForSplit.isNotEmpty()) {
                            append(config.configForSplit)
                            append(", ")
                        }

                        append(config.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = when {
                        !required -> TextDecoration.LineThrough
                        else -> TextDecoration.None
                    },
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ConfigIcon(
    config: SplitConfig,
    enable: Boolean
) = Icon(
    painter = painterResource(
        id = when (config) {
            is SplitConfig.Feature -> R.drawable.box
            is SplitConfig.Target -> R.drawable.cpu
            is SplitConfig.Density -> R.drawable.photo
            is SplitConfig.Language -> R.drawable.language
            else -> R.drawable.code
        }
    ),
    contentDescription = null,
    tint = MaterialTheme.colorScheme.onSurfaceVariant
        .copy(alpha = if (enable) 1f else 0.3f)
)