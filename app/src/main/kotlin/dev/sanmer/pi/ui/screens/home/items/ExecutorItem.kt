package dev.sanmer.pi.ui.screens.home.items

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.OverviewCard
import dev.sanmer.pi.ui.utils.stringResource

@Composable
fun ExecutorItem(
    pi: IPackageInfo?,
    onClick: () -> Unit
) = OverviewCard(
    icon = R.drawable.code,
    title = stringResource(id = R.string.home_executor_title),
    enable = false,
    expanded = pi != null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        AppItem(pi = checkNotNull(pi))
    }
}