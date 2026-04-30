package dev.sanmer.pi.ui.screens.install.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.parser.PackageInfoLite

@Composable
fun PackageInfoItem(
    pi: PackageInfoLite,
    size: String? = null
) = OutlinedCard(
    shape = MaterialTheme.shapes.large,
    border = CardDefaults.outlinedCardBorder(false)
) {
    Row(
        modifier = Modifier
            .padding(15.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = pi.iconOrDefault.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(45.dp)
        )

        Column(
            modifier = Modifier.padding(start = 15.dp)
        ) {
            Text(
                text = pi.labelOrDefault,
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = pi.packageName,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = pi.versionName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Text(
                text = buildString {
                    append(pi.compileSdkVersionCodename)
                    size?.let {
                        append(", ")
                        append(it)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
