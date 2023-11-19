package dev.sanmer.pi.ui.screens.home.items

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.ShizukuUtils
import dev.sanmer.pi.ui.component.OverviewCard

@Composable
fun PreferredItem(
    isPreferred: Boolean,
    toggle: () -> Unit
) = OverviewCard(
    icon = R.drawable.ufo,
    title = stringResource(id = R.string.home_default_installer),
    desc = stringResource(id = R.string.home_default_installer_desc),
    trailingIcon = {
        Switch(
            checked = isPreferred,
            onCheckedChange = null
        )
    },
    onClick = toggle,
    enable = ShizukuUtils.isEnable
)