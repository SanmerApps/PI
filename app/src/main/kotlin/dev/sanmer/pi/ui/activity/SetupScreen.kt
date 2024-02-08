package dev.sanmer.pi.ui.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sanmer.pi.R
import dev.sanmer.pi.datastore.Provider

@Composable
fun SetupScreen(
    setProvider: (Provider) -> Unit
) = Column(
    modifier = Modifier
        .background(color = MaterialTheme.colorScheme.background)
        .fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(
        text = stringResource(id = R.string.setup_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(30.dp))
    WorkingModeItem(
        title = stringResource(id = R.string.setup_shizuku_title),
        desc = stringResource(id = R.string.setup_shizuku_desc),
        modifier = Modifier.requiredHeightIn(min = 150.dp),
        onClick = { setProvider(Provider.Shizuku) }
    )

    Spacer(modifier = Modifier.height(20.dp))
    WorkingModeItem(
        title = stringResource(id = R.string.setup_root_title),
        desc = stringResource(id = R.string.setup_root_desc),
        modifier = Modifier.requiredHeightIn(min = 150.dp),
        onClick = { setProvider(Provider.Superuser) }
    )
}

@Composable
private fun WorkingModeItem(
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) = Surface(
    onClick = onClick,
    modifier = Modifier
        .requiredWidth(240.dp)
        .then(modifier),
    tonalElevation = 4.dp,
    border = BorderStroke(1.dp, color = MaterialTheme.colorScheme.outline),
    shape = RoundedCornerShape(15.dp)
) {
    Column(
        modifier = Modifier
            .padding(all = 16.dp)
            .requiredHeightIn(min = 120.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}