package dev.sanmer.pi.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.pi.R
import dev.sanmer.pi.bundle.AbiSplitConfig
import dev.sanmer.pi.bundle.DensitySplitConfig
import dev.sanmer.pi.bundle.FeatureSplitConfig
import dev.sanmer.pi.bundle.LanguageSplitConfig
import dev.sanmer.pi.bundle.SplitConfig
import dev.sanmer.pi.bundle.UnspecifiedSplitConfig
import dev.sanmer.pi.ktx.finishActivity
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.BottomSheetLayout
import dev.sanmer.pi.ui.component.Failed
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.ktx.bottom
import dev.sanmer.pi.viewmodel.InstallViewModel
import dev.sanmer.pi.viewmodel.InstallViewModel.State
import dev.sanmer.pi.viewmodel.InstallViewModel.State.Companion.isReady

@Composable
fun InstallScreen(
    viewModel: InstallViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    BackHandler {
        viewModel.deleteTempDir()
        context.finishActivity()
    }

    BottomSheetLayout(
        bottomBar = { bottomPadding ->
            if (viewModel.state.isReady()) {
                BottomBar(
                    modifier = Modifier
                        .padding(bottomPadding)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    onDeny = viewModel::deleteTempDir,
                    onStart = viewModel::startInstall
                )
            }
        },
        shape = MaterialTheme.shapes.large.bottom(0.dp)
    ) { contentPadding ->
        Crossfade(
            modifier = Modifier
                .padding(contentPadding)
                .padding(all = 20.dp),
            targetState = viewModel.state,
            label = "InstallScreen"
        ) { state ->
            when (state) {
                State.None -> Loading(
                    minHeight = 240.dp
                )

                State.InvalidProvider -> Failed(
                    message = stringResource(id = R.string.install_invalid_provider),
                    minHeight = 240.dp
                )

                State.InvalidPackage -> Failed(
                    message = stringResource(id = R.string.install_invalid_package),
                    minHeight = 240.dp
                )

                else -> InstallContent()
            }
        }
    }
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    onDeny: () -> Unit,
    onStart: () -> Unit
) = Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(20.dp)
) {
    Spacer(modifier = Modifier.weight(1f))

    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            onDeny()
            context.finishActivity()
        }
    ) {
        Text(text = stringResource(id = R.string.install_button_cancel))
    }

    Button(
        onClick = {
            onStart()
            context.finishActivity()
        }
    ) {
        Text(text = stringResource(id = R.string.install_button_install))
    }
}

@Composable
private fun InstallContent(
    modifier: Modifier = Modifier,
    viewModel: InstallViewModel = hiltViewModel()
) = Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    PackageItem(
        archiveInfo = viewModel.archiveInfo,
        versionDiff = viewModel.versionDiff,
        sdkDiff = viewModel.sdkDiff,
        totalSize = viewModel.totalSizeStr
    )

    when {
        viewModel.state == State.AppBundle -> {
            AppBundlesItem(
                configs = viewModel.splitConfigs,
                isRequiredConfig = viewModel::isRequiredConfig,
                toggleSplitConfig = viewModel::toggleSplitConfig
            )
        }

        viewModel.hasSourceInfo -> {
            RequesterItem(
                sourceInfo = viewModel.sourceInfo,
                toggleAuthorized = viewModel::toggleAuthorized
            )
        }
    }
}

@Composable
private fun PackageItem(
    archiveInfo: IPackageInfo,
    versionDiff: String,
    sdkDiff: String,
    totalSize: String
) = TittleItem(
    text = stringResource(id = R.string.install_package_title)
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .padding(15.dp)
                .fillMaxWidth()
        ) {
            val context = LocalContext.current
            AsyncImage(
                modifier = Modifier.size(45.dp),
                model = ImageRequest.Builder(context)
                    .data(archiveInfo)
                    .build(),
                contentDescription = null
            )

            Column(
                modifier = Modifier.padding(start = 15.dp)
            ) {
                Text(
                    text = archiveInfo.appLabel,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = archiveInfo.packageName,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = versionDiff,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = "${sdkDiff}, Size: $totalSize",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun RequesterItem(
    sourceInfo: IPackageInfo,
    toggleAuthorized: () -> Unit,
) = TittleItem(
    text = stringResource(id = R.string.install_requester_title)
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = true,
                    onClick = toggleAuthorized,
                    role = Role.Switch
                )
                .padding(all = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            AsyncImage(
                modifier = Modifier.size(45.dp),
                model = ImageRequest.Builder(context)
                    .data(sourceInfo)
                    .crossfade(true)
                    .build(),
                contentDescription = null
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = sourceInfo.appLabel,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = sourceInfo.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = sourceInfo.isAuthorized,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun AppBundlesItem(
    configs: List<SplitConfig>,
    isRequiredConfig: (SplitConfig) -> Boolean,
    toggleSplitConfig: (SplitConfig) -> Unit
) {
    val featureConfigs by remember {
        derivedStateOf { configs.filterIsInstance<FeatureSplitConfig>() }
    }
    val abiConfigs by remember {
        derivedStateOf { configs.filterIsInstance<AbiSplitConfig>() }
    }
    val densityConfigs by remember {
        derivedStateOf { configs.filterIsInstance<DensitySplitConfig>() }
    }
    val languageConfigs by remember {
        derivedStateOf { configs.filterIsInstance<LanguageSplitConfig>() }
    }
    val unspecifiedConfigs by remember {
        derivedStateOf { configs.filterIsInstance<UnspecifiedSplitConfig>() }
    }

    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (featureConfigs.isNotEmpty()) {
            item {
                TittleItem(text = stringResource(id = R.string.install_config_feature_title))
            }
            items(
                items = featureConfigs,
                key = { it.filename }
            ) {
                SplitConfigItem(
                    config = it,
                    isRequiredConfig = isRequiredConfig,
                    toggleSplitConfig = toggleSplitConfig
                )
            }
        }

        if (abiConfigs.isNotEmpty()) {
            item {
                TittleItem(text = stringResource(id = R.string.install_config_abi_title))
            }
            items(
                items = abiConfigs,
                key = { it.filename }
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
                TittleItem(text = stringResource(id = R.string.install_config_density_title))
            }
            items(
                items = densityConfigs,
                key = { it.filename }
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
                TittleItem(text = stringResource(id = R.string.install_config_language_title))
            }
            items(
                items = languageConfigs,
                key = { it.filename }
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
                TittleItem(text = stringResource(id = R.string.install_config_unspecified_title))
            }
            items(
                items = unspecifiedConfigs,
                key = { it.filename }
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

                        append(config.formattedSize)
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
            is FeatureSplitConfig -> R.drawable.box
            is AbiSplitConfig -> R.drawable.cpu
            is DensitySplitConfig -> R.drawable.photo
            is LanguageSplitConfig -> R.drawable.language
            else -> R.drawable.code
        }
    ),
    contentDescription = null,
    tint = MaterialTheme.colorScheme.onSurfaceVariant
        .copy(alpha = if (enable) 1f else 0.3f)
)

@Composable
internal fun TittleItem(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )

    content()
}