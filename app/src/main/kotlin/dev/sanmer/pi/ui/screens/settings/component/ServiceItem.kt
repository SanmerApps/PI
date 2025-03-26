package dev.sanmer.pi.ui.screens.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.PIService.State
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.SettingNormalItem

@Composable
fun ServiceItem(
    state: State,
    getPlatform: () -> String,
    tryStart: () -> Unit
) = SettingNormalItem(
    icon = when (state) {
        State.Pending -> R.drawable.mood_neutral
        State.Success -> R.drawable.mood_wink
        State.Failure -> R.drawable.mood_xd
    },
    title = stringResource(id = when (state) {
        State.Pending -> R.string.settings_service_starting
        State.Success -> R.string.settings_service_running
        State.Failure -> R.string.settings_service_not_running
    }),
    desc = when (state) {
        State.Pending -> stringResource(id = R.string.settings_service_wait)
        State.Success -> stringResource(
            id = R.string.settings_service_version,
            BuildConfig.VERSION_CODE,
            getPlatform()
        )
        State.Failure -> stringResource(id = R.string.settings_service_try_start)
    },
    onClick = { if (state.isFailed) tryStart() }
)