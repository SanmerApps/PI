package dev.sanmer.pi.ui.screens.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.sanmer.hidden.compat.PackageInfoCompat.isNotEmpty
import dev.sanmer.pi.R
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.model.ISessionInfo
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.component.PageIndicator
import dev.sanmer.pi.ui.component.scrollbar.VerticalFastScrollbar
import dev.sanmer.pi.ui.screens.apps.AppItem
import dev.sanmer.pi.ui.utils.expandedShape
import dev.sanmer.pi.utils.extensions.viewPackage
import dev.sanmer.pi.viewmodel.SessionsViewModel
import kotlinx.coroutines.launch

@Composable
fun SessionsScreen(
    @Suppress("UNUSED_PARAMETER")
    navController: NavController,
    viewModel: SessionsViewModel = hiltViewModel()
) {
    DisposableEffect(viewModel.isProviderAlive) {
        viewModel.registerCallback()
        onDispose { viewModel.unregisterCallback() }
    }

    val list by viewModel.sessions.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val state = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                onAbandonAll = viewModel::abandonAll,
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
                    icon = R.drawable.versions,
                    text = R.string.empty_list,
                )
            }

            LazyColumn(
                state = state
            ) {
                items(
                    items = list,
                    key = { it.sessionId }
                ) {
                    SessionItem(it)
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
private fun SessionItem(
    session: ISessionInfo
) {
    var show by remember { mutableStateOf(false) }
    if (show) ViewPackage(
        session = session,
        onClose = { show = false }
    )

    SessionItem(
        session = session,
        onClick = { show = true }
    )
}

@Composable
private fun ViewPackage(
    session: ISessionInfo,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val viewPackage: (String) -> Unit = { packageName ->
        scope.launch {
            context.viewPackage(packageName)
            state.hide()
            onClose()
        }
    }

    ModalBottomSheet(
        sheetState = state,
        shape = BottomSheetDefaults.expandedShape(20.dp),
        onDismissRequest = onClose,
        windowInsets = WindowInsets.navigationBars,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier.padding(all = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (session.app.isNotEmpty) {
                Card(
                    text = stringResource(id = R.string.install_package_title)
                ) {
                    AppItem(
                        pi = session.app!!.toIPackageInfo(),
                        onClick = { viewPackage(session.appPackageName!!) }
                    )
                }
            }

            if (session.installer.isNotEmpty) {
                Card(
                    text = stringResource(id = R.string.install_executor_title)
                ) {
                    AppItem(
                        pi = session.installer!!.toIPackageInfo(),
                        onClick = { viewPackage(session.installerPackageName!!) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Card(
    modifier: Modifier = Modifier,
    text: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )

        Surface(
            shape = RoundedCornerShape(15.dp),
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outlineVariant),
            content = content
        )
    }
}

@Composable
private fun TopBar(
    onAbandonAll: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = {
        Text(text = stringResource(id = R.string.app_name))
    },
    actions = {
        IconButton(onClick = onAbandonAll) {
            Icon(
                painter = painterResource(id = R.drawable.clear_all),
                contentDescription = null
            )
        }
    },
    scrollBehavior = scrollBehavior
)