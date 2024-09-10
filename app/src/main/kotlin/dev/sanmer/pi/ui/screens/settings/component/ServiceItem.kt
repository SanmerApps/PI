package dev.sanmer.pi.ui.screens.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.SettingNormalItem

@Composable
fun ServiceItem(
    isAlive: Boolean,
    platform: String,
    tryStart: () -> Unit
) = SettingNormalItem(
    icon = when {
        isAlive -> R.drawable.mood_wink
        else -> R.drawable.mood_xd
    },
    title = when {
        isAlive -> stringResource(id = R.string.settings_service_running)
        else -> stringResource(id = R.string.settings_service_not_running)
    },
    desc = when {
        isAlive -> stringResource(
            id = R.string.settings_service_version,
            BuildConfig.VERSION_CODE,
            platform
        )

        else -> stringResource(id = R.string.settings_service_try_start)
    },
    onClick = tryStart
)