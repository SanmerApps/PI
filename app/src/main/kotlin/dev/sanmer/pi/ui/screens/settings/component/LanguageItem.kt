package dev.sanmer.pi.ui.screens.settings.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.BuildCompat
import dev.sanmer.pi.ktx.applicationLocale
import dev.sanmer.pi.ktx.localizedDisplayName
import dev.sanmer.pi.ui.component.SettingNormalItem

@Composable
fun LanguageItem(
    context: Context
) = SettingNormalItem(
    icon = R.drawable.world,
    title = stringResource(id = R.string.settings_language),
    desc = context.applicationLocale?.localizedDisplayName ?: stringResource(id = R.string.settings_language_system),
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