package dev.sanmer.pi.ui.screens.workingmode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.ui.component.NavigateUpTopBar
import dev.sanmer.pi.ui.provider.LocalUserPreferences
import dev.sanmer.pi.ui.screens.workingmode.component.WorkingModeItem
import dev.sanmer.pi.viewmodel.SettingsViewModel

@Composable
fun WorkingModeScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPreferences = LocalUserPreferences.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopBar(
                navController = navController,
                scrollBehavior = scrollBehavior
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WorkingModeItem(
                title = stringResource(id = R.string.setup_root_title),
                desc = stringResource(id = R.string.setup_root_desc),
                selected = userPreferences.provider == Provider.Superuser,
                onClick = { viewModel.setProvider(Provider.Superuser) }
            )

            WorkingModeItem(
                title = stringResource(id = R.string.setup_shizuku_title),
                desc = stringResource(id = R.string.setup_shizuku_desc),
                selected = userPreferences.provider == Provider.Shizuku,
                onClick = { viewModel.setProvider(Provider.Shizuku) }
            )
        }
    }
}

@Composable
private fun TopBar(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) = NavigateUpTopBar(
    title = stringResource(id = R.string.setup_title),
    navController = navController,
    scrollBehavior = scrollBehavior
)