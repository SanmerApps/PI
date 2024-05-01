package dev.sanmer.pi.ui.screens.apps.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.CollapsingTopAppBar
import dev.sanmer.pi.ui.screens.apps.AppItem
import dev.sanmer.pi.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun ViewScreen(
    navController: NavController,
    viewModel: AppViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

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

        }
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
    scrollBehavior = scrollBehavior
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
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (appOps.isOpenable) {
            FilledTonalIconButton(
                onClick = appOps::launch
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.window_maximize),
                    contentDescription = null
                )
            }
        }

        if (appOps.isUninstallable) {
            val scope = rememberCoroutineScope()
            FilledTonalIconButton(
                onClick = {
                    scope.launch {
                        if (appOps.uninstall()) {
                            onBack()
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.trash),
                    contentDescription = null
                )
            }
        }

        FilledTonalIconButton(
            onClick = appOps::export
        ) {
            Icon(
                painter = painterResource(id = R.drawable.package_export),
                contentDescription = null
            )
        }
    }
}