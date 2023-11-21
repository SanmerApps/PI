package dev.sanmer.pi.ui.screens.home

import android.content.pm.PackageInfo
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.ui.screens.home.items.AppItem
import dev.sanmer.pi.ui.utils.expandedShape

@Composable
fun AppList(
    onDismiss: () -> Unit,
    packages: List<PackageInfo>,
    onChoose: (PackageInfo) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        shape = BottomSheetDefaults.expandedShape(15.dp),
        windowInsets = WindowInsets.navigationBars
    ) {
        Text(
            text = stringResource(id = R.string.home_select_app_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        LazyColumn(
            modifier = Modifier.padding(top = 18.dp)
        ) {
            items(
                items = packages,
                key = { it.packageName }
            ) {
                Surface(
                    onClick = {
                        onChoose(it)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(15.dp)
                ) {
                    AppItem(pi = it)
                }
            }
        }
    }
}