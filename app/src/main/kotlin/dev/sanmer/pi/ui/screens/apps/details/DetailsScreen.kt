package dev.sanmer.pi.ui.screens.apps.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.sanmer.pi.ui.component.SettingSwitchItem
import dev.sanmer.pi.ui.screens.apps.AppItem
import dev.sanmer.pi.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(
    navController: NavController,
    viewModel: AppViewModel = hiltViewModel()
) {
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
                icon = R.drawable.file_unknown,
                title = stringResource(id = R.string.details_requester_title),
                desc = stringResource(id = R.string.details_requester_desc),
                checked = viewModel.packageInfo.isRequester,
                onChange = viewModel::setRequester
            )

            SettingSwitchItem(
                icon = R.drawable.code,
                title = stringResource(id = R.string.details_executor_title),
                desc = stringResource(id = R.string.details_executor_desc),
                checked = viewModel.packageInfo.isExecutor,
                onChange = viewModel::setExecutor
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

        if (appOps.isUninstallable) {
            FilledTonalIconButton(
                onClick = { uninstall = true }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.trash),
                    contentDescription = null
                )
            }
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