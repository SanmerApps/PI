package dev.sanmer.pi.ui.screens.install

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.ktx.finishActivity
import dev.sanmer.pi.ui.ktx.isScrollingUp
import dev.sanmer.pi.ui.ktx.plus
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val listState = rememberLazyListState()
    val isScrollingUp by listState.isScrollingUp()

    val onDeny = {
        viewModel.deleteCache()
        context.finishActivity()
    }
    val onStart = {
        viewModel.install()
        context.finishActivity()
    }

    val featureConfigs by remember {
        derivedStateOf { viewModel.splitConfigs.filterIsInstance<SplitConfig.Feature>() }
    }
    val targetConfigs by remember {
        derivedStateOf { viewModel.splitConfigs.filterIsInstance<SplitConfig.Target>() }
    }
    val densityConfigs by remember {
        derivedStateOf { viewModel.splitConfigs.filterIsInstance<SplitConfig.Density>() }
    }
    val languageConfigs by remember {
        derivedStateOf { viewModel.splitConfigs.filterIsInstance<SplitConfig.Language>() }
    }
    val unspecifiedConfigs by remember {
        derivedStateOf { viewModel.splitConfigs.filterIsInstance<SplitConfig.Unspecified>() }
    }

    var select by remember { mutableStateOf(false) }
    if (select) SelectUserItem(
        onDismiss = { select = false },
        user = viewModel.user,
        users = viewModel.users,
        onChange = viewModel::updateUser
    )

    BackHandler(onBack = onDeny)

    Scaffold(
        topBar = {
            TopBar(
                user = viewModel.user,
                onSelectUer = { select = true },
                onDeny = onDeny,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isScrollingUp,
                enter = fadeIn() + scaleIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ActionButton(onStart = onStart)
            }
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = contentPadding + PaddingValues(horizontal = 20.dp, vertical = 10.dp)
        ) {
            if (viewModel.hasSourceInfo) {
                item {
                    TittleItem(text = stringResource(R.string.install_requester_title))
                }
                item {
                    PackageInfoItem(packageInfo = viewModel.sourceInfo)
                }
            }

            item {
                TittleItem(text = stringResource(R.string.install_package_title))
            }
            item {
                PackageInfoItem(
                    packageInfo = viewModel.archiveInfo,
                    versionDiff = viewModel.versionDiff,
                    sdkVersionDiff = viewModel.sdkVersionDiff,
                    fileSize = viewModel.fileSize,
                )
            }

            if (featureConfigs.isNotEmpty()) {
                item {
                    TittleItem(text = stringResource(R.string.install_config_feature_title))
                }
                items(
                    items = featureConfigs,
                    key = { it.file.name }
                ) {
                    SplitConfigItem(
                        config = it,
                        isRequiredConfig = viewModel::isRequiredConfig,
                        toggleSplitConfig = viewModel::toggleSplitConfig
                    )
                }
            }

            if (targetConfigs.isNotEmpty()) {
                item {
                    TittleItem(text = stringResource(R.string.install_config_abi_title))
                }
                items(
                    items = targetConfigs,
                    key = { it.file.name }
                ) {
                    SplitConfigItem(
                        config = it,
                        isRequiredConfig = viewModel::isRequiredConfig,
                        toggleSplitConfig = viewModel::toggleSplitConfig
                    )
                }
            }

            if (densityConfigs.isNotEmpty()) {
                item {
                    TittleItem(text = stringResource(R.string.install_config_density_title))
                }
                items(
                    items = densityConfigs,
                    key = { it.file.name }
                ) {
                    SplitConfigItem(
                        config = it,
                        isRequiredConfig = viewModel::isRequiredConfig,
                        toggleSplitConfig = viewModel::toggleSplitConfig
                    )
                }
            }

            if (languageConfigs.isNotEmpty()) {
                item {
                    TittleItem(text = stringResource(R.string.install_config_language_title))
                }
                items(
                    items = languageConfigs,
                    key = { it.file.name }
                ) {
                    SplitConfigItem(
                        config = it,
                        isRequiredConfig = viewModel::isRequiredConfig,
                        toggleSplitConfig = viewModel::toggleSplitConfig
                    )
                }
            }

            if (unspecifiedConfigs.isNotEmpty()) {
                item {
                    TittleItem(text = stringResource(R.string.install_config_unspecified_title))
                }
                items(
                    items = unspecifiedConfigs,
                    key = { it.file.name }
                ) {
                    SplitConfigItem(
                        config = it,
                        isRequiredConfig = viewModel::isRequiredConfig,
                        toggleSplitConfig = viewModel::toggleSplitConfig
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    onStart: () -> Unit
) = FloatingActionButton(
    onClick = onStart
) {
    Icon(
        painter = painterResource(id = R.drawable.player_play),
        contentDescription = null
    )
}

@Composable
private fun TopBar(
    user: InstallViewModel.UserInfoCompat,
    onSelectUer: () -> Unit,
    onDeny: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(id = R.string.install_activity_label)) },
    navigationIcon = {
        IconButton(
            onClick = onDeny
        ) {
            Icon(
                painter = painterResource(id = R.drawable.x),
                contentDescription = null
            )
        }
    },
    actions = {
        SuggestionChip(
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