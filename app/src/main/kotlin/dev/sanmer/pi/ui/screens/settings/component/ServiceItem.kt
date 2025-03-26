package dev.sanmer.pi.ui.screens.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.SettingNormalItem

@Composable
fun ServiceItem(
    isSucceed: Boolean,
    getPlatform: () -> String,
    tryStart: () -> Unit
) = SettingNormalItem(
    icon = when {
        isSucceed -> R.drawable.mood_wink
        else -> R.drawable.mood_xd
    },
    title = when {
        isSucceed -> stringResource(id = R.string.settings_service_running)
        else -> stringResource(id = R.string.settings_service_not_running)
    },
    desc = when {
        isSucceed -> stringResource(
            id = R.string.settings_service_version,
            BuildConfig.VERSION_CODE,
            getPlatform()
        )

        else -> stringResource(id = R.string.settings_service_try_start)
    },
    onClick = tryStart
)