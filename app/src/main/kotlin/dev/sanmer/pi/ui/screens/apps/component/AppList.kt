package dev.sanmer.pi.ui.screens.apps.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.R
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Default.toIPackageInfo
import dev.sanmer.pi.ui.component.MenuChip
import dev.sanmer.pi.ui.ktx.bottom
import dev.sanmer.pi.viewmodel.AppsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppList(
    list: List<IPackageInfo>,
    listState: LazyListState,
    settings: (IPackageInfo) -> AppsViewModel.Settings,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var packageName by remember { mutableStateOf("") }
    if (packageName.isNotEmpty()) BottomSheet(
        onDismiss = { packageName = "" },
        pi = list.first { it.packageName == packageName },
        settings = settings
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        state = listState,
        contentPadding = contentPadding
    ) {
        items(list) {
            AppItem(
                pi = it,
                onClick = { packageName = it.packageName },
            )
        }
    }
}

@Composable
private fun BottomSheet(
    onDismiss: () -> Unit,
    pi: IPackageInfo,
    settings: (IPackageInfo) -> AppsViewModel.Settings
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large.bottom(0.dp),
        dragHandle = null
    ) {
        AppItem(
            pi = pi.toIPackageInfo(),
            enabled = false
        )

        SettingItem(
            pi = pi,
            settings = settings(pi),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingItem(
    pi: IPackageInfo,
    settings: AppsViewModel.Settings,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) = FlowRow(
    modifier = Modifier
        .padding(contentPadding)
        .clip(shape = MaterialTheme.shapes.medium)
        .border(
            border = CardDefaults.outlinedCardBorder(),
            shape = MaterialTheme.shapes.medium)
        .fillMaxWidth()
        .padding(all = 15.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
    maxItemsInEachRow = 2
) {
    val scope = rememberCoroutineScope()

    MenuChip(
        selected = pi.isRequester,
        enabled = !pi.isRequester,
        onClick = {
            scope.launch {
                settings.setRequester()
            }
        },
        label = { Text(text = stringResource(id = R.string.app_requester)) },
    )

    MenuChip(
        selected = pi.isExecutor,
        enabled = !pi.isExecutor,
        onClick = {
            scope.launch {
                settings.setExecutor()
            }
        },
        label = { Text(text = stringResource(id = R.string.app_executor)) }
    )

    MenuChip(
        selected = pi.isAuthorized,
        enabled = pi.packageName != BuildConfig.APPLICATION_ID,
        onClick = {
            scope.launch {
                settings.setAuthorized()
            }
        },
        label = { Text(text = stringResource(id = R.string.app_authorize)) },
    )
}