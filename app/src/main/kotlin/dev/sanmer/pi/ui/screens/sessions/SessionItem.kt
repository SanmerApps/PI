package dev.sanmer.pi.ui.screens.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.R
import dev.sanmer.pi.model.ISessionInfo
import dev.sanmer.pi.ui.component.LabelItem

@Composable
fun SessionItem(
    session: ISessionInfo
) = Row(
    modifier = Modifier
        .padding(all = 16.dp)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
    val context = LocalContext.current
    val installerLabel by remember {
        derivedStateOf { session.loadInstallerLabel(context) }
    }
    val appLabel by remember {
        derivedStateOf { session.loadAppLabel(context) }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppIconItem(data = session.installer)

        Icon(
            painter = painterResource(id = R.drawable.arrow_narrow_down),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AppIconItem(data = session.appIcon())
    }

    Column(
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = installerLabel.toString(),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = session.installerPackageName.toString(),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = appLabel.toString(),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = session.appPackageName.toString(),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LabelItem(text = "ID: ${session.sessionId}")

            if (session.isActive) {
                LabelItem(text = "ACTIVE")
            }

            if (session.isStaged) {
                LabelItem(text = "STAGED")
            }

            if (session.isCommitted) {
                LabelItem(text = "COMMITTED")
            }
        }
    }
}

@Composable
private fun AppIconItem(
    data: Any?
) {
    val context = LocalContext.current

    AsyncImage(
        modifier = Modifier.size(30.dp),
        model = ImageRequest.Builder(context)
            .data(data)
            .fallback(android.R.drawable.sym_def_app_icon)
            .crossfade(true)
            .build(),
        contentDescription = null
    )
}