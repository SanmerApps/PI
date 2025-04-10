package dev.sanmer.pi.ui.screens.apps.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.VersionCompat.getSdkVersion
import dev.sanmer.pi.compat.VersionCompat.versionStr
import dev.sanmer.pi.model.IPackageInfo

@Composable
fun AppItem(
    pi: IPackageInfo,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) = Row(
    modifier = Modifier
        .clickable(enabled = enabled, onClick = onClick)
        .padding(all = 15.dp)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
    val context = LocalContext.current
    AsyncImage(
        modifier = Modifier.size(40.dp),
        model = ImageRequest.Builder(context)
            .data(pi)
            .crossfade(true)
            .build(),
        contentDescription = null
    )

    Column(
        modifier = Modifier.weight(1f),
    ) {
        Text(
            text = pi.appLabel,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = pi.packageName,
            style = MaterialTheme.typography.bodyMedium
        )

        val versionStr by remember {
            derivedStateOf { pi.versionStr }
        }
        Text(
            text = versionStr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        val sdkVersion by remember {
            derivedStateOf { pi.getSdkVersion(context) }
        }
        Text(
            text = sdkVersion,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (pi.isRequester) LabelText(
                text = stringResource(id = R.string.app_action_requester)
            )
            if (pi.isExecutor) LabelText(
                text = stringResource(id = R.string.app_action_executor)
            )
            if (pi.isAuthorized) LabelText(
                text = stringResource(id = R.string.app_action_authorized)
            )
        }
    }
}

@Composable
private fun LabelText(
    text: String,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer
) = Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier = Modifier
        .background(
            color = backgroundColor,
            shape = CircleShape
        )
        .padding(horizontal = 8.dp, vertical = 2.dp)
)