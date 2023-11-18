package dev.sanmer.pi.ui.screens.home.items

import androidx.compose.runtime.Composable
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.ShizukuUtils
import dev.sanmer.pi.ui.component.OverviewCard
import dev.sanmer.pi.ui.utils.stringResource

@Composable
fun ShizukuItem() = OverviewCard(
    icon = when {
        ShizukuUtils.isGranted -> R.drawable.circle_check
        else -> R.drawable.alert_circle
    },
    title = when {
        !ShizukuUtils.isAlive -> stringResource(id = R.string.home_shizuku_not_running)
        !ShizukuUtils.atLeast12 -> stringResource(id = R.string.home_shizuku_low_version)
        ShizukuUtils.isGranted -> stringResource(id = R.string.home_shizuku_access, R.string.home_shizuku_granted)
        else -> stringResource(id = R.string.home_shizuku_access, R.string.home_shizuku_not_authorized)
    },
    desc = when {
        ShizukuUtils.isGranted -> ShizukuUtils.version
        else -> null
    },
    onClick = { ShizukuUtils.requestPermission() },
    enable = !ShizukuUtils.isEnable
)