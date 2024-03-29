package dev.sanmer.pi.ui.screens.apps.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.model.IPackageInfo

@Composable
fun AppItem(
    pi: IPackageInfo,
    onClick: () -> Unit
) = Row(
    modifier = Modifier
        .clickable(
            enabled = true,
            onClick = onClick,
            role = Role.Switch
        )
        .padding(all = 16.dp)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    val context = LocalContext.current

    AsyncImage(
        modifier = Modifier.size(45.dp),
        model = ImageRequest.Builder(context)
            .data(pi.inner)
            .crossfade(true)
            .build(),
        contentDescription = null
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .weight(1f)
    ) {
        Text(
            text = pi.label,
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = pi.packageName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Switch(
        checked = pi.authorized,
        onCheckedChange = null
    )
}