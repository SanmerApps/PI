package dev.sanmer.pi.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sanmer.mrepo.ui.component.HtmlText
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.R
import dev.sanmer.pi.app.Const
import dev.sanmer.pi.ui.navigation.navigateToApps
import dev.sanmer.pi.ui.screens.home.items.AuthorizedAppItem
import dev.sanmer.pi.ui.screens.home.items.ExecutorItem
import dev.sanmer.pi.ui.screens.home.items.RequesterItem
import dev.sanmer.pi.ui.screens.home.items.StateItem
import dev.sanmer.pi.ui.utils.ProvideMenuShape
import dev.sanmer.pi.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val authorized by viewModel.authorized.collectAsStateWithLifecycle(0)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(viewModel.isProviderAlive) {
        viewModel.loadData()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                onReset = viewModel::resetWorkingMode,
                onInit = viewModel::providerInit,
                onDestroy = viewModel::providerDestroy,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StateItem(
                isAlive = viewModel.isProviderAlive,
                version = viewModel.providerVersion,
                platform = viewModel.providerPlatform
            )

            AuthorizedAppItem(
                count = authorized,
                isProviderAlive = viewModel.isProviderAlive,
                onClick = { navController.navigateToApps() }
            )

            var isRequester by remember { mutableStateOf(false) }
            RequesterItem(
                pi = viewModel.requester,
                onClick = { isRequester = true}
            )

            var isExecutor by remember { mutableStateOf(false) }
            ExecutorItem(
                pi = viewModel.executor,
                onClick = { isExecutor = true}
            )

            if (isRequester || isExecutor) {
                AppList(
                    onDismiss = {
                        if (isRequester) isRequester = false
                        if (isExecutor) isExecutor = false
                    },
                    packages = viewModel.packages,
                    onChoose = when {
                        isRequester -> viewModel::setRequesterPackage
                        isExecutor -> viewModel::setExecutorPackage
                        else -> throw IllegalStateException()
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    onReset: () -> Unit,
    onInit: () -> Unit,
    onDestroy: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(id = R.string.app_name)) },
    actions = {
        var expanded by remember { mutableStateOf(false) }
        IconButton(
            onClick = { expanded = true }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.dots_vertical),
                contentDescription = null
            )

            var show by remember { mutableStateOf(false) }
            Menu(
                expanded = expanded,
                onReset = onReset,
                onInit = onInit,
                onDestroy = onDestroy,
                onClose = { expanded = false },
                onAbout = { show = true }
            )

            if (show) AboutDialog(
                onClose = { show = false }
            )
        }
    },
    scrollBehavior = scrollBehavior
)

@Composable
private fun Menu(
    expanded: Boolean,
    onReset: () -> Unit,
    onInit: () -> Unit,
    onDestroy: () -> Unit,
    onClose: () -> Unit,
    onAbout: () -> Unit
) = ProvideMenuShape {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onClose,
        offset = DpOffset(0.dp, 10.dp)
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.home_menu_reset_mode)) },
            onClick = {
                onReset()
                onClose()
            }
        )

        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.home_menu_restart_service)) },
            onClick = {
                onInit()
                onClose()
            }
        )

        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.home_menu_stop_service)) },
            onClick = {
                onDestroy()
                onClose()
            }
        )

        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.home_menu_about)) },
            onClick = {
                onAbout()
                onClose()
            }
        )
    }
}

@Composable
private fun AboutDialog(
    onClose: () -> Unit
) = AlertDialog(
    onDismissRequest = onClose
) {
    Surface(
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            val context = LocalContext.current
            AsyncImage(
                modifier = Modifier.size(40.dp),
                model = ImageRequest.Builder(context)
                    .data(R.mipmap.launcher)
                    .crossfade(true)
                    .build(),
                contentDescription = null
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                HtmlText(
                    text = stringResource(id = R.string.home_about_view_source,
                        "<b><a href=\"${Const.GITHUB_URL}\">GitHub</a></b>"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}