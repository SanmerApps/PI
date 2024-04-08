package dev.sanmer.pi.ui.activity

import android.content.pm.PackageInfo
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.screens.apps.AppItem
import dev.sanmer.pi.ui.utils.expandedShape
import dev.sanmer.pi.ui.utils.stringResource
import dev.sanmer.pi.viewmodel.InstallViewModel

@Composable
fun InstallScreen(
    viewModel: InstallViewModel = hiltViewModel(),
    onFinish: () -> Unit
) {
    val onDeny: () -> Unit = {
        viewModel.deleteTempDir()
        onFinish()
    }

    ModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDeny,
        scrimColor = Color.Transparent,
        shape = BottomSheetDefaults.expandedShape(20.dp),
        windowInsets = WindowInsets.statusBars,
        dragHandle = null
    ) {
        Crossfade(
            targetState = viewModel.isReady,
            label = "InstallScreen"
        ) { isReady ->
            if (isReady) {
                InstallContent(
                    viewModel = viewModel,
                    onDeny = onDeny,
                    onFinish = onFinish
                )
            } else {
                Loading(minHeight = 200.dp)
            }
        }
    }
}

@Composable
private fun InstallContent(
    viewModel: InstallViewModel,
    onDeny: () -> Unit,
    onFinish: () -> Unit
) = Column(
    modifier = Modifier
        .padding(top = 20.dp, bottom = 15.dp)
        .padding(horizontal = 15.dp)
        .fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(15.dp),
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
        viewModel.isAppBundle -> {
            AppBundlesItem(
                configs = viewModel.splitConfigs,
                isRequiredConfig = viewModel::isRequiredConfig,
                toggleSplitConfig = viewModel::toggleSplitConfig,
                modifier = Modifier.weight(1f)
            )
        }
        viewModel.hasSourceInfo -> {
            RequesterItem(
                sourceInfo = viewModel.sourceInfo,
                toggleAuthorized = viewModel::toggleAuthorized
            )
        }
    }

    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()
    Row(
        modifier = Modifier
            .padding(navigationBarsPadding)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onDeny
        ) {
            Text(text = stringResource(id = R.string.button_cancel))
        }

        Button(
            onClick = {
                viewModel.startInstall()
                onFinish()
            }
        ) {
            Text(text = stringResource(id = R.string.button_install))
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
    text = stringResource(id = R.string.package_title)
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
                modifier = Modifier.size(50.dp),
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
    text = stringResource(id = R.string.home_requester_title)
) {
    OutlinedCard(
        shape = RoundedCornerShape(15.dp)
    ) {
        AppItem(
            pi = sourceInfo,
            onClick = toggleAuthorized
        )
    }
}

@Composable
private fun AppBundlesItem(
    configs: List<SplitConfig>,
    isRequiredConfig: (SplitConfig) -> Boolean,
    toggleSplitConfig: (SplitConfig) -> Unit,
    modifier: Modifier = Modifier
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

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (featureConfigs.isNotEmpty()) {
            item {
                TittleItem(text = stringResource(id = R.string.config_feature_title))
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
                TittleItem(text = stringResource(id = R.string.config_abi_title))
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
                TittleItem(text = stringResource(id = R.string.config_density_title))
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
                TittleItem(text = stringResource(id = R.string.config_language_title))
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
                TittleItem(text = stringResource(id = R.string.config_unspecified_title))
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
    val disabled by remember {
        derivedStateOf { config.isDisabled() }
    }
    val required by remember {
        derivedStateOf { isRequiredConfig(config) }
    }

    Surface(
        shape = RoundedCornerShape(15.dp),
        onClick = { toggleSplitConfig(config) },
        enabled = !disabled,
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
                    text = "${config.filename}, ${config.formattedSize()}",
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