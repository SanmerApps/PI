package dev.sanmer.pi.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.Const
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.ktx.applicationLocale
import dev.sanmer.pi.ktx.localizedDisplayName
import dev.sanmer.pi.ktx.viewUrl
import dev.sanmer.pi.ui.component.NavigateUpTopBar
import dev.sanmer.pi.ui.component.SettingNormalItem
import dev.sanmer.pi.ui.ktx.navigateSingleTopTo
import dev.sanmer.pi.ui.main.Screen
import dev.sanmer.pi.ui.provider.LocalUserPreferences
import dev.sanmer.pi.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userPreferences = LocalUserPreferences.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            ServiceItem(
                isAlive = viewModel.isProviderAlive,
                platform = viewModel.providerPlatform,
                tryStart = viewModel::tryStartProvider
            )

            SettingNormalItem(
                icon = R.drawable.command,
                title = stringResource(id = R.string.setup_title),
                desc = stringResource(
                    id = when (userPreferences.provider) {
                        Provider.Superuser -> R.string.setup_root_title
                        Provider.Shizuku -> R.string.setup_shizuku_title
                        else -> R.string.unknown_error
                    }
                ),
                onClick = {
                    navController.navigateSingleTopTo(Screen.WorkingMode.route)
                }
            )

            LanguageItem(
                context = context
            )

            SettingNormalItem(
                icon = R.drawable.language,
                title = stringResource(id = R.string.settings_translation),
                desc = stringResource(id = R.string.settings_translation_desc),
                onClick = {
                    context.viewUrl(Const.TRANSLATE_URL)
                }
            )

            SettingNormalItem(
                icon = R.drawable.brand_github,
                title = stringResource(id = R.string.settings_source_code),
                desc = Const.GITHUB_URL,
                onClick = {
                    context.viewUrl(Const.GITHUB_URL)
                }
            )
        }
    }
}

@Composable
private fun ServiceItem(
    isAlive: Boolean,
    platform: String,
    tryStart: () -> Unit
) = SettingNormalItem(
    icon = when {
        isAlive -> R.drawable.mood_wink
        else -> R.drawable.mood_puzzled
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

@Composable
private fun LanguageItem(
    context: Context
) = SettingNormalItem(
    icon = R.drawable.world,
    title = stringResource(id = R.string.settings_language),
    desc = context.applicationLocale?.localizedDisplayName
        ?: stringResource(id = R.string.settings_language_system),
    onClick = {
        // noinspection InlinedApi
        context.startActivity(
            Intent(
                Settings.ACTION_APP_LOCALE_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )
    },
    enabled = BuildCompat.atLeastT
)

@Composable
private fun TopBar(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) = NavigateUpTopBar(
    title = stringResource(id = R.string.settings_title),
    navController = navController,
    scrollBehavior = scrollBehavior
)
