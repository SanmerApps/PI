package dev.sanmer.pi.ui.screens.home.items

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.OverviewCard

@Composable
fun AuthorizedAppItem(
    count: Int,
    isProviderAlive: Boolean,
    onClick: () -> Unit
) = OverviewCard(
    icon = R.drawable.settings,
    title = pluralStringResource(id = R.plurals.home_authorized_apps_count, count = count, count),
    desc = stringResource(id = R.string.home_view_authorized_apps),
    onClick = onClick,
    enable = isProviderAlive
)