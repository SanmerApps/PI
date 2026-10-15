package dev.sanmer.pi.ui.main

import android.content.pm.PackageInfo
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.fallback
import dev.sanmer.pi.R
import dev.sanmer.pi.core.compat.PackageInfoCompat.compileSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.minSdkVersion
import dev.sanmer.pi.core.compat.PackageInfoCompat.targetSdkVersion
import dev.sanmer.pi.datastore.model.Preference
import dev.sanmer.pi.ui.component.Logo
import dev.sanmer.pi.ui.ktx.surface
import kotlinx.coroutines.launch

@Composable
fun PackagesContent(
    viewModel: PackagesViewModel,
    contentPadding: PaddingValues
) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) {
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    viewModel.packageInfos.onSuccess { packageInfos ->
        PackageInfoList(
            state = viewModel.listState,
            packageInfos = packageInfos,
            preference = viewModel.preference,
            setOriginatingPackageName = viewModel::setOriginatingPackageName,
            setInstallerPackageName = viewModel::setInstallerPackageName,
            contentPadding = contentPadding + PaddingValues(top = SearchBarDefaults.InputFieldHeight)
        )
    }

    SearchBar(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(contentPadding),
        state = searchBarState,
        inputField = {
            SearchBarDefaults.InputField(
                textFieldState = viewModel.query,
                searchBarState = searchBarState,
                onSearch = {},
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.magnifying_glass),
                        contentDescription = null
                    )
                },
                trailingIcon = if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    {
                        IconButton(
                            onClick = {
                                scope.launch { searchBarState.animateToCollapsed() }
                                viewModel.query.clearText()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.x),
                                contentDescription = null
                            )
                        }
                    }
                } else null
            )
        }
    )
}

@Composable
private fun PackageInfoList(
    state: LazyListState,
    packageInfos: List<PackageInfo>,
    preference: Preference,
    setOriginatingPackageName: (String) -> Unit,
    setInstallerPackageName: (String) -> Unit,
    contentPadding: PaddingValues
) = LazyColumn(
    state = state,
    contentPadding = PaddingValues(15.dp) + contentPadding,
    verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterVertically)
) {
    items(
        items = packageInfos,
        key = { it.packageName }
    ) { packageInfo ->
        var expanded by rememberSaveable(packageInfo.packageName) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .surface(
                    shape = MaterialTheme.shapes.large,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder(false)
                )
                .clickable(
                    onClick = { expanded = !expanded },
                )
                .animateContentSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PackageInfoItem(
                packageInfo = packageInfo,
                isOriginating = packageInfo.packageName == preference.originatingPackageName,
                isInstaller = packageInfo.packageName == preference.installerPackageName,
                modifier = Modifier.fillMaxWidth()
            )

            if (expanded) Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { setOriginatingPackageName(packageInfo.packageName) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.folder_simple),
                        contentDescription = null
                    )
                }

                FilledTonalIconButton(
                    onClick = { setInstallerPackageName(packageInfo.packageName) }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.box_arrow_down),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageInfoItem(
    packageInfo: PackageInfo,
    isOriginating: Boolean,
    isInstaller: Boolean,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(15.dp)
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(packageInfo)
            .fallback(R.drawable.question_mark)
            .build(),
        contentDescription = null,
        modifier = Modifier.size(48.dp)
    )

    Column(
        modifier = Modifier.weight(1f)
    ) {
        val context = LocalContext.current
        val appLabel = remember(packageInfo.packageName, packageInfo.longVersionCode) {
            packageInfo.applicationInfo?.loadLabel(context.packageManager)?.toString()
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = appLabel ?: packageInfo.packageName,
                style = MaterialTheme.typography.titleMedium
            )

            if (isOriginating) Logo(
                painter = painterResource(R.drawable.folder_simple_fill),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            if (isInstaller) Logo(
                painter = painterResource(R.drawable.box_arrow_down_fill),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        if (appLabel != null) Text(
            text = packageInfo.packageName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = remember(packageInfo.packageName, packageInfo.longVersionCode) {
                packageInfo.versionDisplay()
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = remember(packageInfo.packageName, packageInfo.longVersionCode) {
                packageInfo.sdkVersion()
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun PackageInfo.versionDisplay() = "$versionName ($longVersionCode)"

private fun PackageInfo.sdkVersion() = buildAnnotatedString {
    val valueStyle = SpanStyle(fontWeight = FontWeight.Normal)
    append("Target: ")
    withStyle(valueStyle) { append("$targetSdkVersion") }
    append(" Min: ")
    withStyle(valueStyle) { append("$minSdkVersion") }
    append(" Compile: ")
    withStyle(valueStyle) { append("$compileSdkVersion") }
    append(" UID: ")
    withStyle(valueStyle) { append("${applicationInfo?.uid}") }
}