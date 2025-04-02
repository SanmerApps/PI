package dev.sanmer.pi.ui.screens.settings.component

import android.os.Process
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.R
import dev.sanmer.pi.model.ServiceState
import dev.sanmer.pi.ui.component.SettingNormalItem
import dev.sanmer.su.IServiceManager

@Composable
fun ServiceItem(
    state: ServiceState,
    restart: () -> Unit
) = SettingNormalItem(
    icon = when (state) {
        ServiceState.Pending -> R.drawable.mood_neutral
        is ServiceState.Success -> R.drawable.mood_wink
        is ServiceState.Failure -> R.drawable.mood_xd
    },
    title = stringResource(id = when (state) {
        ServiceState.Pending -> R.string.settings_service_starting
        is ServiceState.Success -> R.string.settings_service_running
        is ServiceState.Failure -> R.string.settings_service_not_running
    }),
    desc = when (state) {
        ServiceState.Pending -> stringResource(id = R.string.settings_service_wait)
        is ServiceState.Success -> stringResource(
            id = R.string.settings_service_version,
            BuildConfig.VERSION_CODE,
            state.service.platform
        )
        is ServiceState.Failure -> stringResource(id = R.string.settings_service_try_start)
    },
    onClick = { if (state.isFailed) restart() }
)

private val IServiceManager.platform
    inline get() = when (uid) {
        Process.ROOT_UID -> "root"
        Process.SHELL_UID -> "adb"
        else -> "unknown (${uid})"
    }