package dev.sanmer.pi.ui.screens.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.app.Const
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.datastore.Provider
import dev.sanmer.pi.ui.component.SettingNormalItem
import dev.sanmer.pi.ui.component.SettingSwitchItem
import dev.sanmer.pi.ui.navigation.graphs.SettingsScreen
import dev.sanmer.pi.ui.providable.LocalUserPreferences
import dev.sanmer.pi.ui.utils.navigateSingleTopTo
import dev.sanmer.pi.utils.extensions.applicationLocale
import dev.sanmer.pi.utils.extensions.viewUrl
import dev.sanmer.pi.viewmodel.SettingsViewModel
import java.util.Locale

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userPreferences = LocalUserPreferences.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            TittleItem(
                text = stringResource(id = R.string.settings_behavior)
            )

            ServiceItem(
                isAlive = viewModel.isProviderAlive,
                version = viewModel.providerVersion,
                platform = viewModel.providerPlatform,
                tryStart = viewModel::tryStartProvider
            )
            
            SettingNormalItem(
                icon = R.drawable.components,
                title = stringResource(id = R.string.setup_title),
                desc = stringResource(id = when (userPreferences.provider) {
                    Provider.Superuser -> R.string.setup_root_title
                    Provider.Shizuku -> R.string.setup_shizuku_title
                    else -> R.string.unknown_error
                }),
                onClick = {
                    navController.navigateSingleTopTo(SettingsScreen.WorkingMode.route)
                }
            )

            SettingSwitchItem(
                icon = R.drawable.device_mobile_up,
                title = stringResource(id = R.string.settings_self_update),
                desc = stringResource(id = R.string.settings_self_update_desc),
                checked = userPreferences.selfUpdate,
                onChange = viewModel::setSelfUpdate,
            )

            HorizontalDivider()

            TittleItem(
                text = stringResource(id = R.string.settings_interface)
            )

            LanguageItem(
                context = context
            )

            SettingSwitchItem(
                icon = R.drawable.color_swatch,
                title = stringResource(id = R.string.settings_dynamic_color),
                desc = stringResource(id = R.string.settings_dynamic_color_desc),
                checked = userPreferences.dynamicColor,
                onChange = viewModel::setDynamicColor,
                enabled = BuildCompat.atLeastS
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
private fun TittleItem(
    text: String,
    modifier: Modifier = Modifier
) = Text(
    modifier = modifier.padding(all = 16.dp),
    text = text,
    style = MaterialTheme.typography.titleSmall
)

@Composable
private fun ServiceItem(
    isAlive: Boolean,
    version: Int,
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
        isAlive -> stringResource(id = R.string.settings_service_version, version, platform)
        else -> stringResource(id = R.string.settings_service_try_start)
    },
    onClick = tryStart
)

@SuppressLint("InlinedApi")
@Composable
private fun LanguageItem(
    context: Context
) = SettingNormalItem(
    icon = R.drawable.world,
    title = stringResource(id = R.string.settings_language),
    desc = context.applicationLocale?.localizedDisplayName ?: stringResource(id = R.string.settings_language_system),
    onClick = {
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
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = {
        Text(text = stringResource(id = R.string.app_name))
    },
    scrollBehavior = scrollBehavior
)

private val Locale.localizedDisplayName: String
    get() = getDisplayName(this)
        .replaceFirstChar {
            if (it.isLowerCase()) {
                it.titlecase(this)
            } else {
                it.toString()
            }
        }
