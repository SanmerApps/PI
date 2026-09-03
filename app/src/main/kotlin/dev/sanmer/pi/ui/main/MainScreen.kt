package dev.sanmer.pi.ui.main

import android.content.pm.UserInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sanmer.pi.R
import dev.sanmer.pi.core.parser.IPackageInfo
import dev.sanmer.pi.core.parser.SplitConfig
import dev.sanmer.pi.ktx.formatFileSize
import dev.sanmer.pi.ktx.sdkVersionDiff
import dev.sanmer.pi.ktx.versionDiff
import dev.sanmer.pi.model.LoadData
import dev.sanmer.pi.ui.component.FilterItem
import dev.sanmer.pi.ui.component.LabelText
import dev.sanmer.pi.ui.ktx.plus
import dev.sanmer.pi.ui.ktx.surface
import dev.sanmer.pi.ui.main.MainViewModel.Content

@Composable
fun MainScreen(
    viewModel: MainViewModel
) = AnimatedContent(
    modifier = Modifier
        .animateContentSize()
        .background(
            color = MaterialTheme.colorScheme.background
        )
        .fillMaxSize(),
    targetState = viewModel.content,
    transitionSpec = {
        slideIntoContainer(
            towards = with(SlideDirection) {
                if (viewModel.content == Content.Main) Down else Up
            },
            animationSpec = tween(600)
        ) togetherWith slideOutOfContainer(
            towards = with(SlideDirection) {
                if (viewModel.content == Content.Main) Down else Up
            },
            animationSpec = tween(600)
        )
    },
    contentAlignment = Alignment.Center
) { content ->
    val context = LocalContext.current
    when (content) {
        Content.Main -> MainContent(
            viewModel = viewModel,
            contentPadding = WindowInsets.systemBars.asPaddingValues()
        )

        is Content.Apks -> {
            BackHandler { viewModel.content = Content.Main }
            ApksContent(
                base = content.packageInfo.base,
                onApks = { viewModel.install(context, content.uri, content.packageInfo) },
                splitConfigs = content.packageInfo.splitConfigs,
                isSplitSelected = { viewModel.isSplitSelected(content.uri, it) },
                onPickSplit = { viewModel.pickSplit(content.uri, it) },
                contentPadding = WindowInsets.systemBars.asPaddingValues()
            )
        }

        is Content.Zip -> {
            BackHandler { viewModel.content = Content.Main }
            ZipContent(
                fileNames = viewModel.fileNames(content.uri),
                packageInfo = content.packageInfos::getValue,
                onZip = { apk, fileName -> viewModel.install(context, content.uri, apk, fileName) },
                contentPadding = WindowInsets.systemBars.asPaddingValues()
            )
        }
    }
}

@Composable
private fun MainContent(
    viewModel: MainViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnimatedContent(
        modifier = modifier,
        targetState = state,
        transitionSpec = {
            slideIntoContainer(
                towards = SlideDirection.Up,
                animationSpec = tween(600)
            ) togetherWith slideOutOfContainer(
                towards = SlideDirection.Up,
                animationSpec = tween(600)
            )
        },
        contentAlignment = Alignment.Center
    ) { state ->
        state.onSuccess {
            if (viewModel.uris.isNotEmpty()) PackageInfoList(
                users = viewModel.users,
                isUserSelected = viewModel::isUserSelected,
                onPickUser = viewModel::pickUser,
                uris = viewModel.uris,
                packageInfo = viewModel::packageInfo,
                fileNames = viewModel::fileNames,
                contentPadding = contentPadding,
                onApk = { uri, apk -> viewModel.install(context, uri, apk) },
                onApks = { uri, apks -> viewModel.install(context, uri, apks) },
                onZip = { uri, apk, fileName -> viewModel.install(context, uri, apk, fileName) },
                onViewApks = { uri, apks -> viewModel.content = Content.Apks(uri, apks) },
                onViewZip = { uri, zip -> viewModel.content = Content.Zip(uri, zip.packageInfos) }
            ) else Placeholder(
                painter = painterResource(R.drawable.seal_check_fill),
                contentPadding = contentPadding,
                tint = MaterialTheme.colorScheme.primary,
                enabled = false
            )
        }.onFailure {
            Placeholder(
                painter = painterResource(R.drawable.seal_warning_fill),
                contentPadding = contentPadding,
                tint = MaterialTheme.colorScheme.error,
                onClick = viewModel::launchSu
            )
        }
    }
}

@Composable
private fun Placeholder(
    painter: Painter,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) = Box(
    modifier = modifier
        .padding(contentPadding)
        .fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier
            .size(60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 60.dp),
                enabled = enabled,
                onClick = onClick
            )
    )
}

@Composable
private fun PackageInfoList(
    users: List<UserInfo>,
    isUserSelected: (UserInfo) -> Boolean,
    onPickUser: (UserInfo) -> Unit,
    uris: List<Uri>,
    packageInfo: (Uri) -> LoadData<IPackageInfo>,
    fileNames: (Uri) -> List<String>,
    onApk: (Uri, IPackageInfo.Apk) -> Unit,
    onApks: (Uri, IPackageInfo.Apks) -> Unit,
    onZip: (Uri, IPackageInfo.Apk, String) -> Unit,
    onViewApks: (Uri, IPackageInfo.Apks) -> Unit,
    onViewZip: (Uri, IPackageInfo.Zip) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) = LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(15.dp) + contentPadding,
    verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterVertically),
    reverseLayout = true
) {
    item {
        if (users.size > 1) FlowRow(
            modifier = Modifier
                .surface(
                    shape = MaterialTheme.shapes.large,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder(false)
                )
                .padding(15.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            users.forEach {
                FilterItem(
                    selected = isUserSelected(it),
                    onClick = { onPickUser(it) },
                    label = it.name
                )
            }
        }
    }

    items(
        items = uris,
        key = { it }
    ) { uri ->
        AnimatedContent(
            targetState = packageInfo(uri),
            transitionSpec = {
                scaleIn(
                    animationSpec = tween(500)
                ) togetherWith scaleOut(
                    animationSpec = tween(500)
                )
            },
            contentAlignment = Alignment.Center
        ) { packageInfo ->
            packageInfo.onLoading {
                LinearProgressIndicator(
                    modifier = Modifier
                        .surface(
                            shape = MaterialTheme.shapes.large,
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            border = CardDefaults.outlinedCardBorder(false)
                        )
                        .padding(15.dp)
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }.onSuccess { packageInfo ->
                when (packageInfo) {
                    is IPackageInfo.Apk -> PackageInfoItem(
                        packageInfo = packageInfo,
                        onClick = { onApk(uri, packageInfo) },
                        label = "APK"
                    )

                    is IPackageInfo.Apks -> PackageInfoItem(
                        packageInfo = packageInfo.base,
                        onClick = { onApks(uri, packageInfo) },
                        onLongClick = { onViewApks(uri, packageInfo) },
                        label = "APKS"
                    )

                    is IPackageInfo.Zip -> {
                        val fileNames = fileNames(uri)
                        val first by remember(uri, fileNames) {
                            derivedStateOf {
                                packageInfo.packageInfos.getValue(fileNames.first())
                            }
                        }
                        PackageInfoItem(
                            packageInfo = first,
                            onClick = { onZip(uri, first, fileNames.first()) },
                            onLongClick = { onViewZip(uri, packageInfo) },
                            label = "ZIP"
                        )
                    }
                }
            }.onFailure { error ->
                val text by remember(uri) {
                    derivedStateOf {
                        error.stackTraceToString()
                    }
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .surface(
                            shape = MaterialTheme.shapes.large,
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            border = CardDefaults.outlinedCardBorder(false)
                        )
                        .padding(15.dp)
                )
            }
        }
    }
}

@Composable
private fun PackageInfoItem(
    packageInfo: IPackageInfo.Apk,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    label: String = ""
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .surface(
            shape = MaterialTheme.shapes.large,
            backgroundColor = MaterialTheme.colorScheme.surface,
            border = CardDefaults.outlinedCardBorder(false)
        )
        .combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
        .padding(15.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(15.dp)
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Image(
            bitmap = packageInfo.packageInfo.iconOrDefault.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(45.dp)
        )

        if (label.isNotEmpty()) LabelText(
            text = label
        )
    }

    Column {
        Text(
            text = packageInfo.packageInfo.labelOrDefault,
            style = MaterialTheme.typography.titleMedium
        )

        if (packageInfo.packageInfo.label != null) Text(
            text = packageInfo.packageInfo.packageName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val version by remember(
            packageInfo.packageInfo.packageName,
            packageInfo.currentPackageInfo?.packageName
        ) {
            derivedStateOf {
                packageInfo.currentPackageInfo versionDiff packageInfo.packageInfo
            }
        }
        Text(
            text = version,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val sdkVersion by remember(
            packageInfo.packageInfo.packageName,
            packageInfo.currentPackageInfo?.packageName
        ) {
            derivedStateOf {
                (packageInfo.currentPackageInfo sdkVersionDiff packageInfo.packageInfo) +
                        " Size: ${packageInfo.sizeBytes.formatFileSize()}"
            }
        }
        Text(
            text = sdkVersion,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ApksContent(
    base: IPackageInfo.Apk,
    onApks: () -> Unit,
    splitConfigs: List<SplitConfig>,
    isSplitSelected: (SplitConfig) -> Boolean,
    onPickSplit: (SplitConfig) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) = LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(15.dp) + contentPadding,
    verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterVertically),
) {
    item {
        PackageInfoItem(
            packageInfo = base,
            onClick = onApks
        )
    }

    items(
        items = splitConfigs,
        key = { it.fileName }
    ) { splitConfig ->
        SplitConfigItem(
            splitConfig = splitConfig,
            isSelected = isSplitSelected(splitConfig),
            onClick = { onPickSplit(splitConfig) }
        )
    }
}

@Composable
private fun SplitConfigItem(
    splitConfig: SplitConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .surface(
            shape = MaterialTheme.shapes.large,
            backgroundColor = MaterialTheme.colorScheme.surface,
            border = CardDefaults.outlinedCardBorder(false)
        )
        .clickable(
            enabled = !splitConfig.isDisabled,
            onClick = onClick
        )
        .padding(15.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(15.dp)
) {
    Icon(
        painter = painterResource(
            when (splitConfig.type) {
                SplitConfig.Type.Feature -> R.drawable.subtract
                is SplitConfig.Type.Abi -> R.drawable.cpu
                is SplitConfig.Type.Density -> R.drawable.image
                is SplitConfig.Type.Language -> R.drawable.translate
                SplitConfig.Type.Unspecified -> R.drawable.question_mark
            }
        ),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.size(30.dp)
    )

    Column(
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = splitConfig.name,
            style = MaterialTheme.typography.titleMedium
        )

        val size by remember(splitConfig.fileName) {
            derivedStateOf {
                splitConfig.sizeBytes.formatFileSize()
            }
        }

        if (splitConfig.configForSplit.isEmpty()) {
            Text(
                text = size,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = splitConfig.configForSplit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = size,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    when {
        isSelected -> Icon(
            painter = painterResource(R.drawable.check_circle_fill),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )

        splitConfig.isDisabled -> Icon(
            painter = painterResource(R.drawable.x_circle_fill),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun ZipContent(
    fileNames: List<String>,
    packageInfo: (String) -> IPackageInfo.Apk,
    onZip: (IPackageInfo.Apk, String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) = LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(15.dp) + contentPadding,
    verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterVertically),
) {
    items(
        items = fileNames,
        key = { it }
    ) { fileName ->
        val packageInfo by remember(fileName) {
            derivedStateOf {
                packageInfo(fileName)
            }
        }
        PackageInfoItem(
            packageInfo = packageInfo,
            onClick = { onZip(packageInfo, fileName) }
        )
    }
}