package dev.sanmer.pi.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.sanmer.pi.R

enum class MainScreen(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int
) {
    Apps(
        route = "AppsScreen",
        label = R.string.page_apps,
        icon = R.drawable.list_details,
    ),

    Sessions(
        route = "SessionsScreen",
        label = R.string.page_sessions,
        icon = R.drawable.versions,
    ),

    Settings(
        route = "SettingsScreen",
        label = R.string.page_settings,
        icon = R.drawable.settings,
    )
}
