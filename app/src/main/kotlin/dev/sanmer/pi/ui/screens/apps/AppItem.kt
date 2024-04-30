package dev.sanmer.pi.ui.screens.apps

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.compat.VersionCompat
import dev.sanmer.pi.model.IPackageInfo

@Composable
internal fun AppItem(
    pi: IPackageInfo,
    onClick: () -> Unit
) = Row(
    modifier = Modifier
        .clip(RoundedCornerShape(15.dp))
        .clickable(
            enabled = true,
            onClick = onClick,
            role = Role.Switch
        )
        .padding(all = 12.dp)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    val context = LocalContext.current
    AsyncImage(
        modifier = Modifier.size(45.dp),
        model = ImageRequest.Builder(context)
            .data(pi)
            .crossfade(true)
            .build(),
        contentDescription = null
    )

    Column(
        modifier = Modifier
            .padding(start = 12.dp)
            .weight(1f)
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
            text = VersionCompat.getVersion(pi),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Text(
            text = VersionCompat.getSdkVersion(pi),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}