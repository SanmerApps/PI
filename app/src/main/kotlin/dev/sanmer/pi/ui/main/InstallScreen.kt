package dev.sanmer.pi.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.R
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.compat.VersionCompat.sdkVersion
import dev.sanmer.pi.compat.VersionCompat.versionStr
import dev.sanmer.pi.ktx.finishActivity
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.Failed
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.ktx.isScrollingUp
import dev.sanmer.pi.ui.ktx.plus
import dev.sanmer.pi.viewmodel.InstallViewModel
import dev.sanmer.pi.viewmodel.InstallViewModel.State

@Composable
fun InstallScreen(
    viewModel: InstallViewModel = hiltViewModel()
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

    BackHandler(
        onBack = onDeny
    )

    Scaffold(
        topBar = {
            TopBar(
                onDeny = onDeny,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = viewModel.state.isReady && isScrollingUp,
                enter = fadeIn() + scaleIn(),
                exit = scaleOut() + fadeOut(),
                label = "ActionButton"
            ) {
                ActionButton(onStart = onStart)
            }
        }
    ) { contentPadding ->
        Crossfade(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            targetState = viewModel.state,
            label = "InstallScreen"
        ) { state ->
            when (state) {
                State.None -> Loading(
                    modifier = Modifier.padding(contentPadding)
                )

                State.InvalidProvider -> Failed(
                    message = stringResource(id = R.string.install_invalid_provider),
                    modifier = Modifier.padding(contentPadding)
                )

                State.InvalidPackage -> Failed(
                    message = stringResource(id = R.string.install_invalid_package),
                    modifier = Modifier.padding(contentPadding)
                )

                else -> InstallContent(
                    listState = listState,
                    contentPadding = contentPadding
                )
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
    onDeny: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(id = R.string.install_activity)) },
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
    scrollBehavior = scrollBehavior
)

@Composable
private fun InstallContent(
    viewModel: InstallViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
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

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = contentPadding + PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    ) {
        if (viewModel.hasSourceInfo) {
            item {
                TittleItem(text = stringResource(R.string.install_requester_title))
            }
            item {
                PackageInfo(packageInfo = viewModel.sourceInfo)
            }
        }

        item {
            TittleItem(text = stringResource(R.string.install_package_title))
        }
        item {
            PackageInfo(
                packageInfo = viewModel.archiveInfo,
                versionDiff = viewModel.versionDiff,
                sdkDiff = viewModel.sdkDiff,
                totalSize = viewModel.totalSizeStr
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

@Composable
private fun PackageInfo(
    packageInfo: IPackageInfo,
    versionDiff: String? = null,
    sdkDiff: String? = null,
    totalSize: String? = null
) = OutlinedCard(
    shape = MaterialTheme.shapes.large,
) {
    Row(
        modifier = Modifier
            .padding(15.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        AsyncImage(
            modifier = Modifier.size(45.dp),
            model = ImageRequest.Builder(context)
                .data(packageInfo)
                .build(),
            contentDescription = null
        )

        Column(
            modifier = Modifier.padding(start = 15.dp)
        ) {
            Text(
                text = packageInfo.appLabel,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = packageInfo.packageName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = versionDiff ?: packageInfo.versionStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = buildString {
                    append(sdkDiff ?: packageInfo.sdkVersion)
                    totalSize?.let { append(", Size: $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun SplitConfigItem(
    config: SplitConfig,
    isRequiredConfig: (SplitConfig) -> Boolean,
    toggleSplitConfig: (SplitConfig) -> Unit,
) {
    val required by remember {
        derivedStateOf { isRequiredConfig(config) }
    }

    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        onClick = { toggleSplitConfig(config) },
        enabled = !config.isDisabled
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIcon(
                config = config,
                enable = required
            )

            Column(
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        !required -> MaterialTheme.colorScheme.outline
                        else -> Color.Unspecified
                    }
                )

                Text(
                    text = buildString {
                        if (config.isConfigForSplit) {
                            append(config.configForSplit)
                            append(", ")
                        }

                        append(config.displaySize)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = when {
                        !required -> TextDecoration.LineThrough
                        else -> TextDecoration.None
                    },
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun ConfigIcon(
    config: SplitConfig,
    enable: Boolean
) = Icon(
    painter = painterResource(
        id = when (config) {
            is SplitConfig.Feature -> R.drawable.box
            is SplitConfig.Target -> R.drawable.cpu
            is SplitConfig.Density -> R.drawable.photo
            is SplitConfig.Language -> R.drawable.language
            else -> R.drawable.code
        }
    ),
    contentDescription = null,
    tint = MaterialTheme.colorScheme.onSurfaceVariant
        .copy(alpha = if (enable) 1f else 0.3f)
)

@Composable
private fun TittleItem(
    text: String,
) = Text(
    text = text,
    style = MaterialTheme.typography.titleMedium
)