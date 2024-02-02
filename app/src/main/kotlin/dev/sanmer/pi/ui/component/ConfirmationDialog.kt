package dev.sanmer.pi.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    message: @Composable () -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
    properties: DialogProperties = DialogProperties(),
) = BasicAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    properties = properties
) {
    Surface(
        modifier = modifier,
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                icon()
            }

            ProvideTextStyle(
                value = MaterialTheme.typography.headlineSmall
                    .copy(fontSize = 20.sp)
            ) {
                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .padding(bottom = 24.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    message()
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                buttons()
            }
        }
    }
}

@Composable
private fun ButtonsCommon(
    text: String,
    onClick: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier
) = Surface(
    onClick = onClick,
    modifier = modifier,
    shape = shape,
    color = MaterialTheme.colorScheme.primaryContainer
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeightIn(min = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier.padding(16.dp),
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ConfirmationButtonsTop(
    text: String,
    onClick: () -> Unit,
) = ButtonsCommon(
    text = text,
    onClick = onClick,
    shape = RoundedCornerShape(
        topStart = 12.dp, topEnd = 12.dp,
        bottomStart = 4.dp, bottomEnd = 4.dp
    )
)

@Composable
fun ConfirmationButtonsCenter(
    text: String,
    onClick: () -> Unit,
) = ButtonsCommon(
    text = text,
    onClick = onClick,
    shape = RoundedCornerShape(4.dp)
)

@Composable
fun ConfirmationButtonsBottom(
    text: String,
    onClick: () -> Unit,
) = ButtonsCommon(
    text = text,
    onClick = onClick,
    shape = RoundedCornerShape(
        topStart = 4.dp, topEnd = 4.dp,
        bottomStart = 12.dp, bottomEnd = 12.dp
    )
)