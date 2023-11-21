package dev.sanmer.pi.ui.screens.home.items

import android.content.pm.PackageInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.ShizukuUtils
import dev.sanmer.pi.ui.component.OverviewCard
import dev.sanmer.pi.ui.utils.stringResource

@Composable
fun RequesterItem(
    pi: PackageInfo?,
    onClick: () -> Unit
) = OverviewCard(
    icon = R.drawable.gift,
    title = stringResource(id = R.string.home_requester_title),
    enable = false,
    expanded = pi != null
) {
    Surface(
        onClick = onClick,
        enabled = ShizukuUtils.isEnable,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        AppItem(pi = pi!!)
    }
}
