package dev.sanmer.pi.ui.screens.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.component.PageIndicator
import dev.sanmer.pi.ui.component.scrollbar.VerticalFastScrollbar
import dev.sanmer.pi.viewmodel.AppsViewModel

@Composable
fun AppsScreen(
    @Suppress("UNUSED_PARAMETER")
    navController: NavController,
    viewModel: AppsViewModel = hiltViewModel()
) {
    DisposableEffect(viewModel) {
        viewModel.loadData()
        onDispose {}
    }

    val list by viewModel.apps.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val state = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
        ) {
            if (viewModel.isLoading) {
                Loading()
            }

            if (list.isEmpty() && !viewModel.isLoading) {
                PageIndicator(
                    icon = R.drawable.list_details,
                    text = R.string.apps_empty,
                )
            }

            LazyColumn(
                state = state
            ) {
                items(
                    items = list,
                    key = { it.packageName }
                ) {
                    AppItem(
                        pi = it,
                        onClick = { }
                    )
                }
            }

            VerticalFastScrollbar(
                state = state,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = {
        Text(text = stringResource(id = R.string.page_apps))
    },
    scrollBehavior = scrollBehavior
)