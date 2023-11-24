package dev.sanmer.pi.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.app.Const
import dev.sanmer.pi.compat.ShizukuCompat
import dev.sanmer.pi.compat.ActivityCompat
import dev.sanmer.pi.compat.PackageManagerCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.service.InstallService.Companion.startInstallService
import dev.sanmer.pi.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class InstallActivity : ComponentActivity() {
    private val tempFile by lazy { externalCacheDir!!.resolve(Const.TEMP_PACKAGE) }

    private var sourceInfo: PackageInfo? by mutableStateOf(null)
    private var isAuthorized by mutableStateOf(false)
    private var archiveInfo: PackageInfo? by mutableStateOf(null)

    @Inject
    lateinit var localRepository: LocalRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("InstallActivity onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initPackage(intent)

        setContent {
            if (isAuthorized && archiveInfo != null) {
                onOneTime()
                return@setContent
            }

            AppTheme {
                if (!isAuthorized) {
                    InstallScreen(
                        sourceInfo = sourceInfo,
                        archiveInfo = archiveInfo,
                        onAlways = ::onAlways,
                        onOneTime = ::onOneTime,
                        onDeny = ::onDeny
                    )
                }
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
                launch { localRepository.insert(pi) }.join()
            }
        }
        onOneTime()
    }

    private fun onOneTime() {
        startInstallService(
            packageFile = tempFile
        )

        finish()
    }

    private fun onDeny() {
        tempFile.delete()
        finish()
    }

    private fun initPackage(intent: Intent?) = lifecycleScope.launch {
        if (!ShizukuCompat.isEnable) {
            Timber.w("Shizuku not running")
            finish()
            return@launch
        }

        val packageUri = intent?.data
        if (packageUri == null) {
            Timber.i("Failed to get packageUri")
            finish()
            return@launch
        }

        withContext(Dispatchers.IO) {
            val callingPackage = ActivityCompat.getReferrer(this@InstallActivity)
            sourceInfo = getSourceInfo(callingPackage)
            isAuthorized = localRepository.getByPackageInfo(sourceInfo)

            Timber.i("From ${sourceInfo?.packageName} (${isAuthorized})")
        }

        withContext(Dispatchers.IO) {
            val input = contentResolver.openInputStream(packageUri)
            if (input == null) {
                Timber.e("Failed to create inputStream")
                return@withContext
            }

            input.use {
                tempFile.outputStream().use {output ->
                    input.copyTo(output)
                }
            }

            archiveInfo = getArchiveInfo(tempFile)
            if (archiveInfo == null) {
                Timber.e("Failed to get archiveInfo")
                finish()
            }
        }
    }

    private fun getSourceInfo(callingPackage: String?): PackageInfo? {
        if (callingPackage == null) return null
        return runCatching {
            PackageManagerCompat.getPackageInfo(
                callingPackage, 0, 0
            )
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