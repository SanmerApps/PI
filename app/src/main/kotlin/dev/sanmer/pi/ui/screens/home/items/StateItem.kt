package dev.sanmer.pi.ui.screens.home.items

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.OverviewCard

@Composable
fun StateItem(
    isAlive: Boolean,
    version: Int,
    platform: String
) = OverviewCard(
    icon = when {
        isAlive -> R.drawable.circle_check
        else -> R.drawable.alert_circle
    },
    title = when {
        isAlive -> stringResource(id = R.string.home_service_running)
        else -> stringResource(id = R.string.home_service_not_running)
    },
    desc = when {
        isAlive -> stringResource(id = R.string.home_service_version, version, platform)
        else -> null
    },
    enable = false
)