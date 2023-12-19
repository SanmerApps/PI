package dev.sanmer.pi.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.app.Const
import dev.sanmer.pi.app.Settings
import dev.sanmer.pi.compat.PackageInfoCompat.isSystemApp
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.SettingsRepository
import dev.sanmer.pi.service.InstallService
import dev.sanmer.pi.ui.theme.AppTheme
import dev.sanmer.pi.utils.extensions.tmpDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class InstallActivity : ComponentActivity() {
    @Inject lateinit var localRepository: LocalRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val tmpFile by lazy { tmpDir.resolve(Const.TEMP_PACKAGE) }
    private var sourceInfo: PackageInfo? by mutableStateOf(null)
    private var archiveInfo: PackageInfo? by mutableStateOf(null)
    private val isSelf get() = sourceInfo?.packageName == archiveInfo?.packageName

    private var started by mutableStateOf(false)
    private var isAuthorized by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("InstallActivity onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent.data == null) {
            finish()
        } else {
            initPackage(intent)
        }

        setContent {
            val workingMode by settingsRepository.getWorkingModeOrNone()
                .collectAsStateWithLifecycle(initialValue = Settings.Provider.None)

            LaunchedEffect(workingMode) {
                if (!ProviderCompat.isAlive) {
                    ProviderCompat.init(workingMode)
                }
            }

            LaunchedEffect(archiveInfo, ProviderCompat.isAlive) {
                if (archiveInfo == null) return@LaunchedEffect
                if (!ProviderCompat.isAlive) return@LaunchedEffect

                if (isAuthorized) {
                    onOneTime()
                }
            }

            AppTheme {
                InstallScreen(
                    sourceInfo = sourceInfo,
                    archiveInfo = archiveInfo,
                    isProviderAlive = ProviderCompat.isAlive,
                    isAuthorized = isAuthorized,
                    onAlways = ::onAlways,
                    onOneTime = ::onOneTime,
                    onDeny = ::onDeny
                )
            }
        }
    }

    override fun onDestroy() {
        Timber.d("InstallActivity onDestroy")
        setResult(Activity.RESULT_OK)
        super.onDestroy()
    }

    private fun onAlways() {
        lifecycleScope.launch {
            sourceInfo?.let {
                val pi = IPackageInfo(it, true)
                localRepository.insert(pi)
            }

            onOneTime()
        }
    }

    private fun onOneTime() {
        if (!started) {
            InstallService.start(
                context = this,
                archiveFilePath = tmpFile.path,
                archivePackageInfo = archiveInfo!!
            )

            started = true
            finish()
        }
    }

    private fun onDeny() {
        tmpFile.delete()
        finish()
    }

    private fun initPackage(intent: Intent) = lifecycleScope.launch {
        val packageUri = checkNotNull(intent.data)

        withContext(Dispatchers.IO) {
            val sourcePackage = getSourcePackageForHost(packageUri)
            sourceInfo = getSourceInfo(sourcePackage)
            isAuthorized = localRepository.getByPackageInfo(sourceInfo)
        }

        withContext(Dispatchers.IO) {
            contentResolver.openInputStream(packageUri)?.use { input ->
                tmpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            archiveInfo = getArchiveInfo(tmpFile)
            if (archiveInfo == null) {
                finish()
            }

            isAuthorized = isAuthorized or isSelf
        }
    }

    private fun getSourcePackageForHost(uri: Uri): String? {
        val host = uri.host ?: return null
        return runCatching {
            packageManager.resolveContentProvider(
                host, 0
            )?.packageName
        }.getOrNull()
    }

    private fun getSourceInfo(callingPackage: String?): PackageInfo? {
        if (callingPackage == null) return null
        return runCatching {
            packageManager.getPackageInfo(
                callingPackage, 0
            ).let {
                if (it.isSystemApp) null else it
            }
        }.getOrNull()
    }

    private fun getArchiveInfo(archiveFile: File): PackageInfo? {
        return packageManager.getPackageArchiveInfo(
            archiveFile.path, 0
        )?.also {
            it.applicationInfo.sourceDir = archiveFile.path
            it.applicationInfo.publicSourceDir = archiveFile.path
        }
    }
}