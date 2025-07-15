package dev.sanmer.pi.ui.screens.install.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.ktx.bottom
import dev.sanmer.pi.ui.ktx.surface
import dev.sanmer.pi.ui.screens.install.InstallViewModel.UserInfoCompat

@Composable
fun SelectUserItem(
    onDismiss: () -> Unit,
    user: UserInfoCompat,
    users: List<UserInfoCompat>,
    onChange: (UserInfoCompat) -> Unit
) = ModalBottomSheet(
    onDismissRequest = onDismiss,
    shape = MaterialTheme.shapes.large.bottom(0.dp)
) {
    Text(
        text = stringResource(id = R.string.install_select_user_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    LazyColumn(
        modifier = Modifier.padding(all = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(users) {
            UserItem(
                name = it.name,
                selected = it.id == user.id,
                onClick = {
                    onChange(it)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun UserItem(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) = Row(
    modifier = Modifier
        .surface(
            shape = MaterialTheme.shapes.medium,
            backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                elevation = if (selected) 3.dp else 0.dp
            ),
            border = CardDefaults.outlinedCardBorder()
        )
        .clickable(enabled = !selected, onClick = onClick)
        .padding(all = 15.dp)
        .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    Icon(
        painter = painterResource(id = R.drawable.user),
        contentDescription = null
    )

    Text(
        text = name,
        style = MaterialTheme.typography.labelLarge
    )
}