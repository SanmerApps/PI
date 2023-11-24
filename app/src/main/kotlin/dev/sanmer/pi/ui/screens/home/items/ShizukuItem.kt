package dev.sanmer.pi.ui.screens.home.items

import androidx.compose.runtime.Composable
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.ShizukuCompat
import dev.sanmer.pi.ui.component.OverviewCard
import dev.sanmer.pi.ui.utils.stringResource

@Composable
fun ShizukuItem() = OverviewCard(
    icon = when {
        ShizukuCompat.isGranted -> R.drawable.circle_check
        else -> R.drawable.alert_circle
    },
    title = when {
        !ShizukuCompat.isAlive -> stringResource(id = R.string.home_shizuku_not_running)
        !ShizukuCompat.atLeast12 -> stringResource(id = R.string.home_shizuku_low_version)
        ShizukuCompat.isGranted -> stringResource(id = R.string.home_shizuku_access, R.string.home_shizuku_granted)
        else -> stringResource(id = R.string.home_shizuku_access, R.string.home_shizuku_not_authorized)
    },
    desc = when {
        ShizukuCompat.isGranted -> ShizukuCompat.version
        else -> null
    },
    onClick = { ShizukuCompat.requestPermission() },
    enable = !ShizukuCompat.isEnable
)