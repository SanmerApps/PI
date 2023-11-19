package dev.sanmer.pi.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.sanmer.pi.R
import dev.sanmer.pi.app.utils.ShizukuUtils
import dev.sanmer.pi.ui.component.Logo
import dev.sanmer.pi.ui.component.OverviewCard
import dev.sanmer.pi.ui.navigation.navigateToApps
import dev.sanmer.pi.ui.screens.home.items.AuthorizedAppItem
import dev.sanmer.pi.ui.screens.home.items.PreferredItem
import dev.sanmer.pi.ui.screens.home.items.ShizukuItem
import dev.sanmer.pi.viewmodel.HomeViewModel
import timber.log.Timber

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    LaunchedEffect(key1 = ShizukuUtils.isEnable) {
        if (ShizukuUtils.isEnable) viewModel.getPreferred()
    }

    val authorized by viewModel.authorized.collectAsStateWithLifecycle(0)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShizukuItem()

            AuthorizedAppItem(
                count = authorized,
                onClick = { navController.navigateToApps() }
            )

            PreferredItem(
                isPreferred = viewModel.isPreferred,
                toggle = viewModel::togglePreferred
            )
        }
    }
}

@Composable
private fun TopBar(
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(id = R.string.app_name)) },
    navigationIcon = {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            Logo(
                icon = R.drawable.launcher_outline,
                modifier = Modifier.size(32.dp),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                containerColor = MaterialTheme.colorScheme.primary,
                fraction = 0.65f
            )
        }
    },
    scrollBehavior = scrollBehavior
)