package dev.sanmer.pi.ui.screens.home.items

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.ui.component.OverviewCard


@Composable
fun StateItem() = OverviewCard(
    icon = when {
        ProviderCompat.isAlive -> R.drawable.circle_check
        else -> R.drawable.alert_circle
    },
    title = when {
        ProviderCompat.isAlive -> stringResource(id = R.string.home_service_running)
        else -> stringResource(id = R.string.home_service_not_running)
    },
    desc = when {
        ProviderCompat.isAlive -> stringResource(id = R.string.home_service_version, ProviderCompat.version, ProviderCompat.platform)
        else -> null
    },
    enable = false
)