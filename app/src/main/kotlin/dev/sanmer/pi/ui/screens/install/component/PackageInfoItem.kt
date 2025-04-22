package dev.sanmer.pi.ui.screens.install.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import dev.sanmer.pi.compat.VersionCompat.getSdkVersion
import dev.sanmer.pi.compat.VersionCompat.versionStr
import dev.sanmer.pi.model.IPackageInfo

@Composable
fun PackageInfoItem(
    packageInfo: IPackageInfo,
    versionDiff: String? = null,
    sdkVersionDiff: String? = null,
    fileSize: String? = null,
    userName: String? = null
) = OutlinedCard(
    shape = MaterialTheme.shapes.large,
) {
    Row(
        modifier = Modifier
            .padding(15.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        AsyncImage(
            modifier = Modifier.size(45.dp),
            model = ImageRequest.Builder(context)
                .data(packageInfo)
                .build(),
            contentDescription = null
        )

        Column(
            modifier = Modifier.padding(start = 15.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = packageInfo.appLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(15.dp))

                if (userName != null) {
                    LabelText(text = userName)
                }
            }

            Text(
                text = packageInfo.packageName,
                style = MaterialTheme.typography.bodyMedium
            )

            val versionStr by remember {
                derivedStateOf { versionDiff ?: packageInfo.versionStr }
            }
            Text(
                text = versionStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            val sdkVersion by remember {
                derivedStateOf { sdkVersionDiff ?: packageInfo.getSdkVersion(context) }
            }
            Text(
                text = buildString {
                    append(sdkVersion)
                    fileSize?.let {
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

@Composable
private fun LabelText(
    text: String
) = Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier = Modifier
        .background(
            color =  MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape
        )
        .padding(horizontal = 8.dp, vertical = 2.dp)
)

