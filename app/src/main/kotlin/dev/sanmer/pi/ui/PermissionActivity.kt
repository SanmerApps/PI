package dev.sanmer.pi.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.main.PermissionScreen
import dev.sanmer.pi.ui.providable.LocalUserPreferences
import dev.sanmer.pi.ui.theme.AppTheme
import dev.sanmer.pi.viewmodel.PermissionViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PermissionActivity : ComponentActivity() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    private val viewModel: PermissionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        when {
            intent.isOk -> init()
            else -> finish()
        }

        setContent {
            val userPreferences by userPreferencesRepository.data
                .collectAsStateWithLifecycle(initialValue = null)

            val preferences = if (userPreferences == null) {
                return@setContent
            } else {
                checkNotNull(userPreferences)
            }

            CompositionLocalProvider(
                LocalUserPreferences provides preferences
            ) {
                AppTheme(
                    dynamicColor = preferences.dynamicColor
                ) {
                    PermissionScreen()
                }
            }
        }
    }

    override fun finish() {
        Intent().apply {
            putExtra(EXTRA_PERMISSIONS, viewModel.permissions.toTypedArray())
            putExtra(EXTRA_PERMISSION_GRANT_RESULTS, viewModel.permissionResults())
            setResult(RESULT_OK, this)
        }

        super.finish()
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
    }

    private fun init() {
        val packageName = checkNotNull(callingPackage)
        val permissions = intent.permissions.toList()

        lifecycleScope.launch {
            if (!viewModel.load(packageName, permissions)) {
                finish()
            }
        }
    }

    companion object {
        const val ACTION_REQUEST_PERMISSIONS = "dev.sanmer.pi.action.REQUEST_PERMISSIONS"
        const val EXTRA_PERMISSIONS = "dev.sanmer.pi.extra.PERMISSIONS"
        const val EXTRA_PERMISSION_GRANT_RESULTS = "dev.sanmer.pi.extra.PERMISSION_GRANT_RESULTS"

        private val Intent.permissions: Array<String>
            get() = getStringArrayExtra(EXTRA_PERMISSIONS) ?: emptyArray()

        private val Intent.isOk: Boolean
            get() = action == ACTION_REQUEST_PERMISSIONS
                    || permissions.isNotEmpty()

    }
}