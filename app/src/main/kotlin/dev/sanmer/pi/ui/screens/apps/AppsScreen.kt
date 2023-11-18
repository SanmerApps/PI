package dev.sanmer.pi.ui.screens.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.NavigateUpTopBar
import dev.sanmer.pi.ui.component.PageIndicator
import dev.sanmer.pi.ui.component.scrollbar.VerticalFastScrollbar
import dev.sanmer.pi.ui.screens.apps.items.AppItem
import dev.sanmer.pi.viewmodel.AppsViewModel

@Composable
fun AppsScreen(
    navController: NavController,
    viewModel: AppsViewModel = hiltViewModel()
) {
    val list by viewModel.apps.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val state = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                onRefresh = viewModel::loadData,
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
        ) {
            if (list.isEmpty()) {
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
                        onClick = { viewModel.toggle(it) }
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
    onRefresh: () -> Unit,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) = NavigateUpTopBar(
    title = stringResource(id = R.string.apps_title),
    actions = {
        IconButton(
            onClick = onRefresh
        ) {
            Icon(
                painter = painterResource(id = R.drawable.refresh),
                contentDescription = null
            )
        }
    },
    navController = navController,
    scrollBehavior = scrollBehavior
)