package dev.sanmer.pi.ui.activity

import android.content.pm.PackageInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.hidden.compat.content.bundle.AbiSplitConfig
import dev.sanmer.hidden.compat.content.bundle.DensitySplitConfig
import dev.sanmer.hidden.compat.content.bundle.FeatureSplitConfig
import dev.sanmer.hidden.compat.content.bundle.LanguageSplitConfig
import dev.sanmer.hidden.compat.content.bundle.SplitConfig
import dev.sanmer.hidden.compat.content.bundle.UnspecifiedSplitConfig
import dev.sanmer.pi.R
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.BottomBarLayout
import dev.sanmer.pi.ui.component.Failed
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.utils.expandedShape
import dev.sanmer.pi.viewmodel.InstallViewModel
import dev.sanmer.pi.viewmodel.InstallViewModel.State

@Composable
fun InstallScreen(
    viewModel: InstallViewModel = hiltViewModel(),
    onFinish: () -> Unit
) {
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    BackHandler {
        viewModel.deleteTempDir()
        onFinish()
    }

    LaunchedEffect(viewModel.state) {
        if (viewModel.state != State.None) {
            bottomSheetState.expand()
        }
    }

    BottomSheetScaffold(
        content = {},
        scaffoldState = scaffoldState,
        sheetPeekHeight = 300.dp,
        sheetDragHandle = null,
        sheetShape = BottomSheetDefaults.expandedShape(20.dp),
        sheetContent = {
            Crossfade(
                targetState = viewModel.state,
                label = "InstallScreen"
            ) { state ->
                when (state) {
                    State.None -> Loading(minHeight = 300.dp)
                    State.InvalidProvider -> Failed(
                        message = stringResource(id = R.string.install_invalid_provider),
                        minHeight = 300.dp
                    )
                    State.InvalidPackage -> Failed(
                        message = stringResource(id = R.string.install_invalid_package),
                        minHeight = 300.dp
                    )
                    else -> InstallContent(
                        onDeny = {
                            viewModel.deleteTempDir()
                            onFinish()
                        },
                        onFinish = onFinish
                    )
                }
            }
        }
    )
}

@Composable
private fun InstallContent(
    viewModel: InstallViewModel = hiltViewModel(),
    onDeny: () -> Unit,
    onFinish: () -> Unit
) = BottomBarLayout(
    bottomBar = { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .padding(bottom = 20.dp)
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onDeny
            ) {
                Text(text = stringResource(id = R.string.install_button_cancel))
            }

            Button(
                onClick = {
                    viewModel.startInstall()
                    onFinish()
                }
            ) {
                Text(text = stringResource(id = R.string.install_button_install))
            }
        }
    },
    contentWindowInsets = WindowInsets.navigationBars
) { innerPadding ->
    Column(
        modifier = Modifier
            .padding(innerPadding)
            .padding(all = 20.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PackageItem(
            archiveInfo = viewModel.archiveInfo,
            archiveLabel = viewModel.archiveLabel,
            versionDiff = viewModel.versionDiff,
            sdkDiff = viewModel.sdkDiff,
            apkSize = viewModel.formattedApkSize
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
}

@Composable
private fun PackageItem(
    archiveInfo: PackageInfo,
    archiveLabel: String,
    versionDiff: AnnotatedString,
    sdkDiff: AnnotatedString,
    apkSize: String
) = TittleItem(
    text = stringResource(id = R.string.install_package_title)
) {
    Surface(
        shape = RoundedCornerShape(15.dp),
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outlineVariant)
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
                    text = archiveLabel,
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
                    text = "${sdkDiff}, Size: $apkSize",
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
        shape = RoundedCornerShape(15.dp)
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

    Surface(
        shape = RoundedCornerShape(15.dp),
        onClick = { toggleSplitConfig(config) },
        enabled = !config.isDisabled,
        border = CardDefaults.outlinedCardBorder()
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
private fun TittleItem(
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