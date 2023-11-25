package dev.sanmer.pi.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.ShizukuCompat
import dev.sanmer.pi.ui.navigation.navigateToApps
import dev.sanmer.pi.ui.screens.home.items.AuthorizedAppItem
import dev.sanmer.pi.ui.screens.home.items.ExecutorItem
import dev.sanmer.pi.ui.screens.home.items.RequesterItem
import dev.sanmer.pi.ui.screens.home.items.ShizukuItem
import dev.sanmer.pi.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val authorized by viewModel.authorized.collectAsStateWithLifecycle(0)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(key1 = ShizukuCompat.isEnable) {
        viewModel.loadData()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
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
            ShizukuItem()

            AuthorizedAppItem(
                count = authorized,
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
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(id = R.string.app_name)) },
    scrollBehavior = scrollBehavior
)