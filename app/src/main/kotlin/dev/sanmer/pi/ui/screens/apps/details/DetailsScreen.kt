package dev.sanmer.pi.ui.screens.apps.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.CollapsingTopAppBar
import dev.sanmer.pi.ui.component.CollapsingTopAppBarDefaults
import dev.sanmer.pi.ui.component.SettingNormalItem
import dev.sanmer.pi.ui.component.SettingSwitchItem
import dev.sanmer.pi.ui.screens.apps.AppItem
import dev.sanmer.pi.ui.utils.expandedShape
import dev.sanmer.pi.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    navController: NavController,
    viewModel: AppViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollBehavior = CollapsingTopAppBarDefaults.scrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                pi = viewModel.packageInfo,
                appOps = viewModel.appOps,
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            if (viewModel.hasOpInstallPackage) {
                SystemItems(
                    isAllowed = viewModel.opInstallPackageAllowed,
                    toggleOp = viewModel::toggleOpInstallPackage
                )
            }

            TittleItem(
                text = stringResource(id = R.string.details_custom_title)
            )

            SettingSwitchItem(
                icon = R.drawable.package_import,
                title = stringResource(id = R.string.details_request_install),
                desc = stringResource(id = R.string.details_request_install_self),
                checked = viewModel.packageInfo.isAuthorized,
                onChange = viewModel::toggleAuthorized
            )

            var showRequester by remember { mutableStateOf(false) }
            if (showRequester) SelectableBottomSheet(
                title = stringResource(id = R.string.details_requester_title),
                onClose = { showRequester = false },
                ops = viewModel.requesterSelectableOps(context)
            )

            SettingNormalItem(
                icon = R.drawable.file_unknown,
                title = stringResource(id = R.string.details_requester_title),
                desc = stringResource(id = R.string.details_requester_desc),
                onClick = { showRequester = true }
            )

            var showExecutor by remember { mutableStateOf(false) }
            if (showExecutor) SelectableBottomSheet(
                title = stringResource(id = R.string.details_executor_title),
                onClose = { showExecutor = false },
                ops = viewModel.executorSelectableOps(context)
            )

            SettingNormalItem(
                icon = R.drawable.code,
                title = stringResource(id = R.string.details_executor_title),
                desc = stringResource(id = R.string.details_executor_desc),
                onClick = { showExecutor = true }
            )
        }
    }
}

@Composable
private fun TittleItem(
    text: String,
    modifier: Modifier = Modifier
) = Text(
    modifier = modifier.padding(all = 16.dp),
    text = text,
    style = MaterialTheme.typography.titleSmall
)

@Composable
private fun SystemItems(
    isAllowed: Boolean,
    toggleOp: (Boolean) -> Unit
) {
    TittleItem(
        text = stringResource(id = R.string.details_system_title)
    )

    SettingSwitchItem(
        icon = R.drawable.package_import,
        title = stringResource(id = R.string.details_request_install),
        desc = stringResource(id = R.string.details_request_install_system),
        checked = isAllowed,
        onChange = toggleOp
    )

    HorizontalDivider()
}

@Composable
private fun SelectableBottomSheet(
    title: String,
    onClose: () -> Unit,
    ops: List<AppViewModel.SelectableOp>
) {
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val onClick: (AppViewModel.SelectableOp) -> Unit = {
        scope.launch {
            it.onClick()
            state.hide()
            onClose()
        }
    }

    ModalBottomSheet(
        sheetState = state,
        shape = BottomSheetDefaults.expandedShape(20.dp),
        onDismissRequest = onClose,
        windowInsets = WindowInsets.navigationBars,
        dragHandle = null
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(all = 20.dp)
        )

        LazyColumn(
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            items(ops) {
                SelectableItem(
                    text = it.text,
                    selected = it.selected,
                    onClick = { onClick(it) }
                )
            }
        }
    }
}

@Composable
private fun SelectableItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) = Row(
    modifier = Modifier
        .background(
            color = when {
                selected -> MaterialTheme.colorScheme.secondaryContainer
                else -> Color.Unspecified
            }
        )
        .clickable(
            enabled = !selected,
            onClick = onClick
        )
        .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = when {
            selected -> MaterialTheme.colorScheme.primary
            else -> Color.Unspecified
        },
        modifier = Modifier.weight(1f)
    )

    if (selected) {
        Icon(
            painter = painterResource(id = R.drawable.circle_check),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TopBar(
    pi: IPackageInfo,
    appOps: AppViewModel.AppOps,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) = CollapsingTopAppBar(
    title = { Text(text = pi.appLabel) },
    content = {
        TopBarContent(
            pi = pi,
            appOps = appOps,
            onBack = { navController.popBackStack() }
        )
    },
    navigationIcon = {
        IconButton(
            onClick = { navController.popBackStack() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.arrow_left),
                contentDescription = null
            )
        }
    },
    scrollBehavior = scrollBehavior,
    colors = CollapsingTopAppBarDefaults.topAppBarColors(
        scrolledContainerColor = MaterialTheme.colorScheme.surface
    )
)

@Composable
private fun TopBarContent(
    pi: IPackageInfo,
    appOps: AppViewModel.AppOps,
    onBack: () -> Unit
) = Column(
    modifier = Modifier.padding(horizontal = 20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    AppItem(
        pi = pi,
        enabled = false,
        iconSize = 60.dp,
        iconEnd = 20.dp,
        contentPaddingValues = PaddingValues(0.dp),
        verticalAlignment = Alignment.Top
    )

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var progress by remember { mutableStateOf(false) }
        if (progress) ProgressIndicatorDialog()

        var uninstall by remember { mutableStateOf(false) }
        if (uninstall) UninstallDialog(
            appLabel = pi.appLabel,
            onClose = { uninstall = false },
            onDelete = {
                scope.launch {
                    if (appOps.uninstall()) {
                        onBack()
                    }
                }
            }
        )

        if (appOps.isOpenable) {
            FilledTonalIconButton(
                onClick = { appOps.launch(context) }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.window_maximize),
                    contentDescription = null
                )
            }
        }

        FilledTonalIconButton(
            onClick = { appOps.view(context) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.eye),
                contentDescription = null
            )
        }

        FilledTonalIconButton(
            onClick = { appOps.setting(context) }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.settings),
                contentDescription = null
            )
        }

        FilledTonalIconButton(
            onClick = { uninstall = true }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.trash),
                contentDescription = null
            )
        }

        FilledTonalIconButton(
            onClick = {
                scope.launch {
                    progress = true
                    appOps.export(context)
                    progress = false
                }
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.package_export),
                contentDescription = null
            )
        }
    }
}

@Composable
private fun ProgressIndicatorDialog() {
    Dialog(
        onDismissRequest = {}
    ) {
        Surface(
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.padding(all = 12.dp)
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.height(5.dp),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun UninstallDialog(
    appLabel: String,
    onClose: () -> Unit,
    onDelete: () -> Unit
) = AlertDialog(
    onDismissRequest = onClose,
    shape = RoundedCornerShape(20.dp),
    title = { Text(text = appLabel) },
    text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.app_dialog_desc1),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = stringResource(id = R.string.app_dialog_desc2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    },
    confirmButton = {
        TextButton(
            onClick = {
                onDelete()
                onClose()
            }
        ) {
            Text(text = stringResource(id = R.string.dialog_ok))
        }
    },
    dismissButton = {
        TextButton(
            onClick = onClose
        ) {
            Text(text = stringResource(id = R.string.dialog_cancel))
        }
    },
)