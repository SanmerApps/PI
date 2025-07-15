package dev.sanmer.pi.ui.screens.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.Failed
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.component.PageIndicator
import dev.sanmer.pi.ui.component.SearchTopBar
import dev.sanmer.pi.ui.ktx.navigateSingleTopTo
import dev.sanmer.pi.ui.main.Screen
import dev.sanmer.pi.ui.screens.apps.component.AppList
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppsScreen(
    navController: NavController,
    viewModel: AppsViewModel = koinViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()

    BackHandler(
        enabled = viewModel.isSearch,
        onBack = viewModel::closeSearch
    )

    DisposableEffect(true) {
        onDispose(viewModel::closeSearch)
    }

    Scaffold(
        topBar = {
            TopBar(
                isSearch = viewModel.isSearch,
                onQueryChange = viewModel::search,
                onOpenSearch = viewModel::openSearch,
                onCloseSearch = viewModel::closeSearch,
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .imePadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (viewModel.isFailed) {
                Failed(
                    error = viewModel.error,
                    modifier = Modifier.padding(contentPadding)
                )
                return@Scaffold
            }

            if (viewModel.isPending) {
                Loading(
                    modifier = Modifier.padding(contentPadding)
                )
            } else if (viewModel.apps.isEmpty() && !viewModel.isQueryEmpty) {
                PageIndicator(
                    icon = R.drawable.list_search,
                    text = R.string.empty_list,
                    modifier = Modifier.padding(contentPadding)
                )
            }

            AppList(
                list = viewModel.apps,
                listState = listState,
                settings = viewModel::settings,
                contentPadding = contentPadding
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
                onClick = { navController.navigateSingleTopTo(Screen.Settings) }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.settings_2),
                    contentDescription = null
                )
            }
        }
    )
}