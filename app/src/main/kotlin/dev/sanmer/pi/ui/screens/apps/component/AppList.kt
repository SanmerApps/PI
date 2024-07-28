package dev.sanmer.pi.ui.screens.apps.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.MenuChip
import dev.sanmer.pi.viewmodel.AppsViewModel
import kotlinx.coroutines.launch

@Composable
fun AppList(
    list: List<IPackageInfo>,
    state: LazyListState,
    buildSettings: (IPackageInfo) -> AppsViewModel.Settings,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) = LazyColumn(
    modifier = Modifier
        .fillMaxWidth()
        .animateContentSize(),
    state = state,
    contentPadding = contentPadding
) {
    items(list) {
        AppItem(
            pi = it,
            buildSettings = buildSettings
        )
    }
}

@Composable
private fun AppItem(
    pi: IPackageInfo,
    buildSettings: (IPackageInfo) -> AppsViewModel.Settings
) {
    var expend by rememberSaveable { mutableStateOf(false) }
    val degrees by animateFloatAsState(
        targetValue = if (expend) 90f else 0f,
        label = "AppItem Icon"
    )

    AppItem(
        pi = pi,
        onClick = { expend = !expend },
        trailing = {
            Icon(
                painter = painterResource(id = R.drawable.chevron_right),
                contentDescription = null,
                modifier = Modifier.rotate(degrees)
            )
        }
    )

    if (expend) SettingItem(
        pi = pi,
        settings = buildSettings(pi),
        onClose = { expend = false }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingItem(
    onClose: () -> Unit,
    pi: IPackageInfo,
    settings: AppsViewModel.Settings
) = Box(
    modifier = Modifier
        .padding(all = 10.dp)
        .clip(shape = MaterialTheme.shapes.medium)
        .border(
            border = CardDefaults.outlinedCardBorder(),
            shape = MaterialTheme.shapes.medium
        )
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        MenuChip(
            selected = pi.isAuthorized,
            onClick = {
                scope.launch {
                    settings.setAuthorized()
                    onClose()
                }
            },
            label = { Text(text = stringResource(id = R.string.app_authorized)) },
        )

        MenuChip(
            selected = pi.isRequester,
            onClick = {
                scope.launch {
                    settings.setRequester()
                }
            },
            label = { Text(text = stringResource(id = R.string.app_requester)) },
        )

        MenuChip(
            selected = pi.isExecutor,
            onClick = {
                scope.launch {
                    settings.setExecutor()
                    onClose()
                }
            },
            label = { Text(text = stringResource(id = R.string.app_executor)) },
        )

        MenuChip(
            selected = false,
            onClick = {
                scope.launch {
                    if (settings.export(context)) {
                        onClose()
                    }
                }
            },
            label = { Text(text = stringResource(id = R.string.app_export)) },
        )
    }
}