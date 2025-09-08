package dev.sanmer.pi.ui.screens.install

import android.content.pm.UserInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.ktx.finishActivity
import dev.sanmer.pi.parser.SplitConfig
import dev.sanmer.pi.ui.ktx.isScrollingUp
import dev.sanmer.pi.ui.ktx.plus
import dev.sanmer.pi.ui.provider.LocalPreference
import dev.sanmer.pi.ui.screens.install.InstallViewModel.LoadState
import dev.sanmer.pi.ui.screens.install.component.PackageInfoItem
import dev.sanmer.pi.ui.screens.install.component.SelectUserItem
import dev.sanmer.pi.ui.screens.install.component.SplitConfigItem
import dev.sanmer.pi.ui.screens.install.component.TittleItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun InstallScreen(
    viewModel: InstallViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val preference = LocalPreference.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val isScrollingUp by listState.isScrollingUp()

    var select by remember { mutableStateOf(false) }
    if (select) SelectUserItem(
        onDismiss = { select = false },
        user = viewModel.user,
        users = viewModel.users,
        onChange = viewModel::user::set
    )

    Scaffold(
        topBar = {
            TopBar(
                isReady = viewModel.isReady,
                user = viewModel.user,
                onSelectUer = { select = true },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isScrollingUp && viewModel.isBundleReady,
                enter = fadeIn() + scaleIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        if (viewModel.isServiceReady) {
                            viewModel.start(context)
                            context.finishActivity()
                        } else {
                            viewModel.recreate(preference.provider)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (viewModel.isServiceReady)
                                R.drawable.player_play
                            else
                                R.drawable.rotate
                        ),
                        contentDescription = null
                    )
                }
            }
        }
    ) { contentPadding ->
        when (val state = viewModel.loadState) {
            is LoadState.Failure -> Failed(
                error = state.error,
                contentPadding = contentPadding,
                scrollBehavior = scrollBehavior
            )

            LoadState.Pending -> Loading(
                contentPadding = contentPadding
            )

            is LoadState.Success -> InstallContent(
                loadState = state,
                size = viewModel.size,
                splitConfigs = viewModel.splitConfigs,
                isRequiredConfig = viewModel::isRequiredConfig,
                toggleSplitConfig = viewModel::toggleSplitConfig,
                listState = listState,
                contentPadding = contentPadding,
                scrollBehavior = scrollBehavior
            )
        }
    }
}

@Composable
private fun Failed(
    error: Throwable,
    contentPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val text by remember {
        derivedStateOf { error.stackTraceToString() }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(contentPadding + PaddingValues(all = 20.dp))
    )
}

@Composable
private fun Loading(
    contentPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 5.dp
        )
    }
}

@Composable
private fun InstallContent(
    loadState: LoadState.Success,
    size: String,
    splitConfigs: List<SplitConfig>,
    isRequiredConfig: (SplitConfig) -> Boolean,
    toggleSplitConfig: (SplitConfig) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val featureConfigs by remember {
        derivedStateOf { splitConfigs.filterIsInstance<SplitConfig.Feature>() }
    }
    val targetConfigs by remember {
        derivedStateOf { splitConfigs.filterIsInstance<SplitConfig.Target>() }
    }
    val densityConfigs by remember {
        derivedStateOf { splitConfigs.filterIsInstance<SplitConfig.Density>() }
    }
    val languageConfigs by remember {
        derivedStateOf { splitConfigs.filterIsInstance<SplitConfig.Language>() }
    }
    val unspecifiedConfigs by remember {
        derivedStateOf { splitConfigs.filterIsInstance<SplitConfig.Unspecified>() }
    }

    LazyColumn(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPadding + PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    ) {
        if (loadState.sourceInfo != null) {
            item {
                TittleItem(text = stringResource(R.string.install_requester_title))
            }
            item {
                PackageInfoItem(loadState.sourceInfo)
            }
        }

        item {
            TittleItem(text = stringResource(R.string.install_package_title))
        }
        item {
            PackageInfoItem(
                pi = loadState.archiveInfo,
                size = size
            )
        }

        if (featureConfigs.isNotEmpty()) {
            item {
                TittleItem(text = stringResource(R.string.install_config_feature_title))
            }
            items(
                items = featureConfigs,
                key = { it.fileName }
            ) {
                SplitConfigItem(
                    config = it,
                    isRequiredConfig = isRequiredConfig,
                    toggleSplitConfig = toggleSplitConfig
                )
            }
        }

        if (targetConfigs.isNotEmpty()) {
            item {
                TittleItem(text = stringResource(R.string.install_config_abi_title))
            }
            items(
                items = targetConfigs,
                key = { it.fileName }
            ) {
                SplitConfigItem(
                    config = it,
                    isRequiredConfig = isRequiredConfig,
                    toggleSplitConfig = toggleSplitConfig
                )
            }
        }

        if (densityConfigs.isNotEmpty()) {
            item {
                TittleItem(text = stringResource(R.string.install_config_density_title))
            }
            items(
                items = densityConfigs,
                key = { it.fileName }
            ) {
                SplitConfigItem(
                    config = it,
                    isRequiredConfig = isRequiredConfig,
                    toggleSplitConfig = toggleSplitConfig
                )
            }
        }

        if (languageConfigs.isNotEmpty()) {
            item {
                TittleItem(text = stringResource(R.string.install_config_language_title))
            }
            items(
                items = languageConfigs,
                key = { it.fileName }
            ) {
                SplitConfigItem(
                    config = it,
                    isRequiredConfig = isRequiredConfig,
                    toggleSplitConfig = toggleSplitConfig
                )
            }
        }

        if (unspecifiedConfigs.isNotEmpty()) {
            item {
                TittleItem(text = stringResource(R.string.install_config_unspecified_title))
            }
            items(
                items = unspecifiedConfigs,
                key = { it.fileName }
            ) {
                SplitConfigItem(
                    config = it,
                    isRequiredConfig = isRequiredConfig,
                    toggleSplitConfig = toggleSplitConfig
                )
            }
        }
    }
}

@Composable
private fun TopBar(
    isReady: Boolean,
    user: UserInfo,
    onSelectUer: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(id = R.string.install_activity_label)) },
    navigationIcon = {
        val context = LocalContext.current
        IconButton(
            onClick = {
                context.finishActivity()
            }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.x),
                contentDescription = null
            )
        }
    },
    actions = {
        if (isReady) SuggestionChip(
            modifier = Modifier
                .height(SuggestionChipDefaults.Height)
                .padding(end = 15.dp),
            shape = CircleShape,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            ),
            onClick = onSelectUer,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.user_circle),
                    contentDescription = null,
                    modifier = Modifier.size(SuggestionChipDefaults.IconSize)
                )
            },
            label = {
                Text(text = user.name)
            }
        )
    },
    scrollBehavior = scrollBehavior
)