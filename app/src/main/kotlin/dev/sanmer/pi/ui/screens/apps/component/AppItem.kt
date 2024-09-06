package dev.sanmer.pi.ui.screens.apps.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.VersionCompat.sdkVersion
import dev.sanmer.pi.compat.VersionCompat.versionStr
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.Logo

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

        Text(
            text = pi.versionStr,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Text(
            text = pi.sdkVersion,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (pi.isRequester) Icon(R.drawable.file_import)
            if (pi.isExecutor) Icon(R.drawable.player_play)
            if (pi.isAuthorized) Icon(R.drawable.shield)
        }
    }
}

@Composable
private fun Icon(
    @DrawableRes icon: Int
) = Logo(
    icon = icon,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    modifier = Modifier.size(30.dp)
)