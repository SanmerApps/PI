package dev.sanmer.pi.ui.screens.home.items

import android.content.pm.PackageInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun AppItem(
    pi: PackageInfo,
    modifier: Modifier = Modifier
) = Row(
    modifier = modifier
        .padding(all = 16.dp)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    val context = LocalContext.current
    val label by remember(pi) {
        derivedStateOf {
            val pm = context.packageManager
            pi.applicationInfo.loadLabel(pm)
        }
    }

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
            .padding(horizontal = 16.dp)
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.toString(),
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = pi.packageName,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}