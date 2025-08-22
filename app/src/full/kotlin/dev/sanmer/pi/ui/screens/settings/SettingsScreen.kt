package dev.sanmer.pi.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import dev.sanmer.pi.Const
import dev.sanmer.pi.R
import dev.sanmer.pi.datastore.model.DarkMode
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.ktx.viewUrl
import dev.sanmer.pi.ui.component.CheckIcon
import dev.sanmer.pi.ui.component.SettingNormalItem
import dev.sanmer.pi.ui.component.SettingSwitchItem
import dev.sanmer.pi.ui.ktx.bottom
import dev.sanmer.pi.ui.provider.LocalPreference
import dev.sanmer.pi.ui.screens.settings.component.LanguageItem
import dev.sanmer.pi.ui.screens.settings.component.ServiceItem
import dev.sanmer.pi.ui.screens.settings.component.WorkingModeItem
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val preference = LocalPreference.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var workingMode by remember { mutableStateOf(false) }
    if (workingMode) WorkingModeBottomSheet(
        onDismiss = { workingMode = false },
        setProvider = viewModel::setProvider
    )

    var darkMode by remember { mutableStateOf(false) }
    if (darkMode) DarkModeBottomSheet(
        onDismiss = { darkMode = false },
        setDarkMode = viewModel::setDarkMode
    )

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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
        ) {
            ServiceItem(
                state = state,
                restart = viewModel::restart
            )

            SettingNormalItem(
                icon = R.drawable.command,
                title = stringResource(R.string.setup_title),
                desc = when (preference.provider) {
                    Provider.Superuser -> stringResource(R.string.setup_root_title)
                    Provider.Shizuku -> stringResource(R.string.setup_shizuku_title)
                    else -> ""
                },
                onClick = { workingMode = true }
            )

            SettingSwitchItem(
                icon = R.drawable.hand_finger_off,
                title = stringResource(R.string.settings_automatic_installation),
                desc = stringResource(R.string.settings_automatic_installation_desc),
                checked = preference.automatic,
                onChange = viewModel::setAutomatic
            )

            SettingNormalItem(
                icon = R.drawable.moon,
                title = stringResource(R.string.settings_dark_mode),
                desc = stringResource(preference.darkMode.text),
                onClick = { darkMode = true }
            )

            LanguageItem(context = context)

            SettingNormalItem(
                icon = R.drawable.language,
                title = stringResource(R.string.settings_translation),
                desc = stringResource(R.string.settings_translation_desc),
                onClick = {},
                enabled = false
            )
        }
    }
}

@Composable
private fun WorkingModeBottomSheet(
    onDismiss: () -> Unit,
    setProvider: (Provider) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) = ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = MaterialTheme.shapes.large.bottom(0.dp)
) {
    val preference = LocalPreference.current

    Text(
        text = stringResource(R.string.setup_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    Column(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WorkingModeItem(
            title = stringResource(R.string.setup_root_title),
            desc = stringResource(R.string.setup_root_desc),
            selected = preference.provider == Provider.Superuser,
            onClick = { setProvider(Provider.Superuser) }
        )

        WorkingModeItem(
            title = stringResource(R.string.setup_shizuku_title),
            desc = stringResource(R.string.setup_shizuku_desc),
            selected = preference.provider == Provider.Shizuku,
            onClick = { setProvider(Provider.Shizuku) }
        )
    }
}

@Composable
private fun DarkModeBottomSheet(
    onDismiss: () -> Unit,
    setDarkMode: (DarkMode) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) = ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = MaterialTheme.shapes.large.bottom(0.dp)
) {
    val preference = LocalPreference.current

    Text(
        text = stringResource(R.string.settings_dark_mode),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 40.dp, start = 40.dp, end = 40.dp)
            .align(Alignment.CenterHorizontally)
    ) {
        DarkMode.entries.forEachIndexed { index, value ->
            SegmentedButton(
                selected = preference.darkMode == value,
                onClick = { setDarkMode(value) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = DarkMode.entries.size
                ),
                icon = { SegmentedButtonDefaults.CheckIcon(preference.darkMode == value) }
            ) {
                Text(text = stringResource(value.text))
            }
        }
    }
}

@Composable
private fun TopBar(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) = TopAppBar(
    title = { Text(text = stringResource(R.string.settings_title)) },
    navigationIcon = {
        IconButton(
            onClick = { navController.navigateUp() }
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_left),
                contentDescription = null
            )
        }
    },
    actions = {
        val context = LocalContext.current
        IconButton(
            onClick = { context.viewUrl(Const.GITHUB_URL) }
        ) {
            Icon(
                painter = painterResource(R.drawable.brand_github),
                contentDescription = null
            )
        }
    },
    scrollBehavior = scrollBehavior
)
