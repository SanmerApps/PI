package dev.sanmer.pi.ui.activity

import android.content.pm.PackageInfo
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.ui.component.ConfirmationButtonsBottom
import dev.sanmer.pi.ui.component.ConfirmationButtonsCenter
import dev.sanmer.pi.ui.component.ConfirmationButtonsTop
import dev.sanmer.pi.ui.component.ConfirmationDialog
import dev.sanmer.pi.ui.component.LoadingDialog
import dev.sanmer.pi.ui.utils.formatStringResource

@Composable
fun InstallScreen(
    sourceInfo: PackageInfo?,
    archiveInfo: PackageInfo?,
    onAlways: () -> Unit,
    onOneTime: () -> Unit,
    onDeny: () -> Unit
) = Crossfade(
    targetState = archiveInfo != null && ProviderCompat.isAlive,
    label = "InstallScreen"
) { isReady ->
    if (isReady) {
        ConfirmationDialog(
            sourceInfo = sourceInfo,
            archiveInfo = checkNotNull(archiveInfo),
            onAlways = onAlways,
            onOneTime = onOneTime,
            onDeny = onDeny
        )
    } else {
        LoadingDialog()
    }
}

@Composable
private fun ConfirmationDialog(
    sourceInfo: PackageInfo?,
    archiveInfo: PackageInfo,
    onAlways: () -> Unit,
    onOneTime: () -> Unit,
    onDeny: () -> Unit
) = ConfirmationDialog(
    onDismissRequest = onDeny,
    icon = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (sourceInfo != null) {
                AppIcon(sourceInfo)

                Icon(
                    painter = painterResource(id = R.drawable.arrow_right),
                    contentDescription = null
                )
            }

            AppIcon(archiveInfo)
        }
    },
    message = {
        val pm = LocalContext.current.packageManager
        val sourceLabel by remember(sourceInfo) {
            derivedStateOf {
                sourceInfo?.applicationInfo?.loadLabel(pm) ?: ""
            }
        }
        val archiveLabel by remember(archiveInfo) {
            derivedStateOf {
                archiveInfo.applicationInfo.loadLabel(pm)
            }
        }

        Text(
            text = formatStringResource(
                style = { it.copy(fontStyle = FontStyle.Italic) },
                id = if (sourceInfo != null) {
                    R.string.confirmation_message
                } else {
                    R.string.confirmation_message_unknown
                }, sourceLabel, archiveLabel),
            textAlign = TextAlign.Center
        )
    },
    buttons = {
        ConfirmationButtonsTop(
            text = stringResource(id = if (sourceInfo != null) {
                R.string.confirmation_allow_always
            } else {
                R.string.confirmation_allow_always_unknown
            }),
            onClick = onAlways
        )

        if (sourceInfo != null) {
            ConfirmationButtonsCenter(
                text = stringResource(id = R.string.confirmation_allow_one_time),
                onClick = onOneTime
            )
        }

        ConfirmationButtonsBottom(
            text = stringResource(id = R.string.confirmation_deny),
            onClick = onDeny
        )
    }
)

@Composable
private fun AppIcon(pi: PackageInfo?) {
    val context = LocalContext.current
    AsyncImage(
        modifier = Modifier.size(40.dp),
        model = ImageRequest.Builder(context)
            .data(pi)
            .build(),
        contentDescription = null
    )
}