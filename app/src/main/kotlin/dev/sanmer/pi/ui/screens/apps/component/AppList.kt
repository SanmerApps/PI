package dev.sanmer.pi.ui.screens.apps.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.ui.component.MenuChip
import dev.sanmer.pi.ui.ktx.bottom
import dev.sanmer.pi.viewmodel.AppsViewModel
import kotlinx.coroutines.launch

@Composable
internal fun AppList(
    list: List<IPackageInfo>,
    state: LazyListState,
    buildSettings: (IPackageInfo) -> AppsViewModel.Settings,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    var packageName by remember { mutableStateOf("") }
    val packageInfo by remember(list, packageName) {
        derivedStateOf {
            list.firstOrNull { it.packageName == packageName }
        }
    }

    packageInfo?.let {
        BottomSheet(
            pi = it,
            onClose = { packageName = "" },
            buildSettings = buildSettings
        )
    }

    LazyColumn(
        modifier = Modifier.animateContentSize(),
        state = state,
        contentPadding = contentPadding
    ) {
        items(
            items = list,
            key = { it.packageName }
        ) { pi ->
            AppItem(
                pi = pi,
                onClick = { packageName = pi.packageName }
            )
        }
    }
}

@Composable
private fun BottomSheet(
    pi: IPackageInfo,
    onClose: () -> Unit,
    buildSettings: (IPackageInfo) -> AppsViewModel.Settings
) = ModalBottomSheet(
    onDismissRequest = onClose,
    dragHandle = null,
    windowInsets = WindowInsets(0.dp),
    shape = MaterialTheme.shapes.large.bottom(0.dp)
) {
    val contentPadding = WindowInsets.navigationBars.asPaddingValues()

    val settings by remember(pi) {
        derivedStateOf { buildSettings(pi) }
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(all = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppItem(
            pi = pi.toIPackageInfo(),
            enabled = false,
            iconSize = 60.dp,
            iconEnd = 20.dp,
            contentPaddingValues = PaddingValues(0.dp),
            verticalAlignment = Alignment.Top
        )

        SettingButtons(
            settings = settings
        )

        SettingItem(
            pi = pi,
            settings = settings
        )
    }
}

@Composable
private fun SettingButtons(
    settings: AppsViewModel.Settings
) = Row(
    verticalAlignment = Alignment.CenterVertically
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (settings.isOpenable) {
        FilledTonalIconButton(
            onClick = { settings.launch(context) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.window_maximize),
                contentDescription = null
            )
        }
    }

    FilledTonalIconButton(
        onClick = { settings.view(context) }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.eye),
            contentDescription = null
        )
    }

    FilledTonalIconButton(
        onClick = {
            scope.launch { settings.export(context) }
        }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.package_export),
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingItem(
    pi: IPackageInfo,
    settings: AppsViewModel.Settings
) = FlowRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    val scope = rememberCoroutineScope()

    MenuChip(
        selected = pi.isAuthorized,
        onClick = {
            scope.launch {
                settings.setAuthorized()
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
            }
        },
        label = { Text(text = stringResource(id = R.string.app_executor)) },
    )
}