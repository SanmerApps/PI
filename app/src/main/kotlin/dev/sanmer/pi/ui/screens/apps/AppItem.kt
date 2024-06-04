package dev.sanmer.pi.ui.screens.apps

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.VersionCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.Logo

@Composable
internal fun AppItem(
    pi: IPackageInfo,
    iconSize: Dp = 45.dp,
    iconEnd: Dp = 12.dp,
    contentPaddingValues: PaddingValues = PaddingValues(12.dp),
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    onClick: () -> Unit = {},
    enabled: Boolean = true
) = Row(
    modifier = Modifier
        .clip(RoundedCornerShape(15.dp))
        .clickable(
            enabled = enabled,
            onClick = onClick
        )
        .padding(contentPaddingValues)
        .fillMaxWidth(),
    verticalAlignment = verticalAlignment
) {
    val context = LocalContext.current
    AsyncImage(
        modifier = Modifier.size(iconSize),
        model = ImageRequest.Builder(context)
            .data(pi)
            .crossfade(true)
            .build(),
        contentDescription = null
    )

    Column(
        modifier = Modifier
            .padding(start = iconEnd)
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

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (pi.isAuthorized) Icon(R.drawable.package_import)
            if (pi.isRequester) Icon(R.drawable.file_unknown)
            if (pi.isExecutor) Icon(R.drawable.code)
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