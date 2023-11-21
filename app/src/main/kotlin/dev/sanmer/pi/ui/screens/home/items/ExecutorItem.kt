package dev.sanmer.pi.ui.screens.home.items

import android.content.pm.PackageInfo
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.ShizukuUtils
import dev.sanmer.pi.ui.component.OverviewCard
import dev.sanmer.pi.ui.utils.stringResource

@Composable
fun ExecutorItem(
    pi: PackageInfo?,
    onClick: () -> Unit
) = OverviewCard(
    icon = R.drawable.code,
    title = stringResource(id = R.string.home_executor_title),
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