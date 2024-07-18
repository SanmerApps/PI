package dev.sanmer.pi.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.sanmer.pi.R
import dev.sanmer.pi.ktx.finishActivity
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.ui.component.BottomSheetLayout
import dev.sanmer.pi.ui.component.Loading
import dev.sanmer.pi.ui.ktx.bottom
import dev.sanmer.pi.ui.screens.apps.component.AppItem
import dev.sanmer.pi.viewmodel.PermissionViewModel

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    BackHandler {
        context.finishActivity()
    }

    BottomSheetLayout(
        bottomBar = {
            if (!viewModel.isLoading) {
                BottomBar(
                    modifier = Modifier
                        .padding(it)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    onGrant = viewModel::grantPermissions
                )
            }
        },
        shape = MaterialTheme.shapes.large.bottom(0.dp)
    ) {
        Crossfade(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(it)
                .padding(all = 20.dp),
            targetState = viewModel.isLoading,
            label = "InstallScreen"
        ) { isLoading ->
            when {
                isLoading -> Loading(
                    minHeight = 240.dp
                )

                else -> PermissionContent()
            }
        }
    }
}

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    onGrant: () -> Unit
) = Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(20.dp)
) {
    Spacer(modifier = Modifier.weight(1f))

    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            context.finishActivity()
        }
    ) {
        Text(text = stringResource(id = R.string.permission_button_deny))
    }

    Button(
        onClick = {
            onGrant()
            context.finishActivity()
        }
    ) {
        Text(text = stringResource(id = R.string.permission_button_grant))
    }
}

@Composable
private fun PermissionContent(
    modifier: Modifier = Modifier,
    viewModel: PermissionViewModel = hiltViewModel()
) = Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    AppItem(
        packageInfo = viewModel.packageInfo
    )

    PermissionsItem(
        permissions = viewModel.permissions,
        isRequiredPermission = viewModel::isRequiredPermission,
        togglePermission = viewModel::togglePermission
    )
}

@Composable
private fun AppItem(
    packageInfo: IPackageInfo,
) = TittleItem(
    text = stringResource(id = R.string.permission_app_title)
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        border = CardDefaults.outlinedCardBorder()
    ) {
        AppItem(
            pi = packageInfo,
            enabled = false,
            iconSize = 45.dp,
            iconEnd = 15.dp,
            contentPaddingValues = PaddingValues(15.dp),
            verticalAlignment = Alignment.Top
        )
    }
}

@Composable
private fun PermissionsItem(
    permissions: List<String>,
    isRequiredPermission: (String) -> Boolean,
    togglePermission: (String) -> Unit
) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TittleItem(text = stringResource(id = R.string.permission_permissions_title))
        }
        items(
            items = permissions
        ) {
            PermissionItem(
                permission = it,
                isRequiredPermission = isRequiredPermission,
                togglePermission = togglePermission
            )
        }
    }
}

@Composable
private fun PermissionItem(
    permission: String,
    isRequiredPermission: (String) -> Boolean,
    togglePermission: (String) -> Unit,
) {
    val required by remember {
        derivedStateOf { isRequiredPermission(permission) }
    }

    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        onClick = { togglePermission(permission) }
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.code),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Text(
                    text = permission,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = when {
                        !required -> TextDecoration.LineThrough
                        else -> TextDecoration.None
                    },
                )
            }
        }
    }
}