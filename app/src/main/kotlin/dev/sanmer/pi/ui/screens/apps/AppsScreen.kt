package dev.sanmer.pi.ui.screens.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.component.PageIndicator
import dev.sanmer.pi.ui.component.SearchTopBar
import dev.sanmer.pi.ui.navigation.MainScreen
import dev.sanmer.pi.ui.utils.navigateSingleTopTo
import dev.sanmer.pi.viewmodel.AppsViewModel

@Composable
fun AppsScreen(
    navController: NavController,
    viewModel: AppsViewModel = hiltViewModel()
) {
    val list by viewModel.apps.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val state = rememberLazyListState()

    BackHandler(
        enabled = viewModel.isSearch,
        onBack = viewModel::closeSearch
    )

    DisposableEffect(true) {
        onDispose(viewModel::closeSearch)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                isSearch = viewModel.isSearch,
                onQueryChange = viewModel::search,
                onOpenSearch = viewModel::openSearch,
                onCloseSearch = viewModel::closeSearch,
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier.padding(innerPadding)
        ) {
            if (viewModel.isLoading) {
                Loading()
            }

            if (list.isEmpty() && !viewModel.isLoading) {
                PageIndicator(
                    icon = R.drawable.list_details,
                    text = R.string.empty_list,
                )
            }

            AppList(
                list = list,
                state = state,
                buildSettings = viewModel::buildSettings
            )
        }
    }
}

@Composable
private fun TopBar(
    isSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    var query by remember { mutableStateOf("") }
    DisposableEffect(isSearch) {
        onDispose { query = "" }
    }

    SearchTopBar(
        isSearch = isSearch,
        query = query,
        onQueryChange = {
            onQueryChange(it)
            query = it
        },
        onClose = onCloseSearch,
        title = { Text(text = stringResource(id = R.string.app_name)) },
        scrollBehavior = scrollBehavior,
        actions = {
            if (!isSearch) {
                IconButton(
                    onClick = onOpenSearch
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = null
                    )
                }
            }

            IconButton(
                onClick = { navController.navigateSingleTopTo(MainScreen.Settings.route) }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = null
                )
            }
        }
    )
}