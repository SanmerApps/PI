package dev.sanmer.pi.ui.screens.sessions

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.component.NavigateUpTopBar
import dev.sanmer.pi.ui.component.PageIndicator
import dev.sanmer.pi.viewmodel.SessionsViewModel

@Composable
fun SessionsScreen(
    navController: NavController,
    viewModel: SessionsViewModel = hiltViewModel()
) {
    DisposableEffect(viewModel) {
        viewModel.loadData()
        onDispose {}
    }

    val list by viewModel.sessions.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val state = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                onAbandonAll = viewModel::abandonAll,
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        }
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
                    key = { it.sessionId }
                ) {
                    SessionItem(session = it)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    onAbandonAll: () -> Unit,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) = NavigateUpTopBar(
    title = stringResource(id = R.string.sessions_title),
    actions = {
        IconButton(onClick = onAbandonAll) {
            Icon(
                painter = painterResource(id = R.drawable.cookie_off),
                contentDescription = null
            )
        }
    },
    navController = navController,
    scrollBehavior = scrollBehavior
)