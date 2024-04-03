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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
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
import dev.sanmer.pi.ui.navigation.graphs.HomeScreen
import dev.sanmer.pi.ui.navigation.navigateToSettings
import dev.sanmer.pi.ui.screens.home.items.AuthorizedAppItem
import dev.sanmer.pi.ui.screens.home.items.ExecutorItem
import dev.sanmer.pi.ui.screens.home.items.RequesterItem
import dev.sanmer.pi.ui.screens.home.items.StateItem
import dev.sanmer.pi.ui.utils.ProvideMenuShape
import dev.sanmer.pi.ui.utils.navigateSingleTopTo
import dev.sanmer.pi.viewmodel.AppListViewModel
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
                navController = navController,
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

            if (viewModel.isProviderAlive) {
                AuthorizedAppItem(
                    count = authorized,
                    onClick = { navController.navigateSingleTopTo(HomeScreen.Apps.route) }
                )
            }

            RequesterItem(
                pi = viewModel.requester,
                openSelector = {
                    navController.navigateSingleTopTo(
                        AppListViewModel.putTarget(
                            AppListViewModel.Target.Requester
                        )
                    )
                },
                enabled = viewModel.isProviderAlive
            )

            ExecutorItem(
                pi = viewModel.executor,
                openSelector = {
                    navController.navigateSingleTopTo(
                        AppListViewModel.putTarget(
                            AppListViewModel.Target.Executor
                        )
                    )
                },
                enabled = viewModel.isProviderAlive
            )
        }
    }
}

@Composable
private fun TopBar(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(id = R.string.app_name)) },
    actions = {
        IconButton(
            onClick = { navController.navigateToSettings() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.settings),
                contentDescription = null
            )
        }

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
                onClose = { expanded = false },
                onSessions = { navController.navigateSingleTopTo(HomeScreen.Sessions.route) },
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
    onClose: () -> Unit,
    onSessions: () -> Unit,
    onAbout: () -> Unit
) = ProvideMenuShape {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onClose,
        offset = DpOffset(0.dp, 10.dp)
    ) {
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.home_menu_view_sessions)) },
            onClick = {
                onSessions()
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
) = BasicAlertDialog(
    onDismissRequest = onClose
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
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