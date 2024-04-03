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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.sanmer.hidden.compat.PackageInfoCompat.isNotEmpty
import dev.sanmer.pi.R
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
        windowInsets = WindowInsets(0)
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
        .padding(bottom = 15.dp)
        .padding(horizontal = 15.dp)
        .fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(15.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    TittleItem(
        text = stringResource(id = R.string.package_title)
    ) {
        Surface(
            shape = RoundedCornerShape(15.dp),
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        ) {
            AppInfo(
                archiveInfo = viewModel.archiveInfo,
                archiveLabel = viewModel.archiveLabel,
                versionDiff = viewModel.versionDiff,
                sdkDiff = viewModel.sdkDiff
            )
        }
    }

    if (viewModel.sourceInfo.inner.isNotEmpty) {
        TittleItem(
            text = stringResource(id = R.string.home_requester_title)
        ) {
            OutlinedCard(
                shape = RoundedCornerShape(15.dp)
            ) {
                AppItem(
                    pi = viewModel.sourceInfo,
                    onClick = viewModel::toggleAuthorized
                )
            }
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
private fun AppInfo(
    archiveInfo: PackageInfo,
    archiveLabel: String,
    versionDiff: AnnotatedString,
    sdkDiff: AnnotatedString
) = Row(
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
            text = sdkDiff,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun TittleItem(
    text: String,
    content: @Composable ColumnScope.() -> Unit
) = Column(
    verticalArrangement = Arrangement.spacedBy(4.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )

    content()
}