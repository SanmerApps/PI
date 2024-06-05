package dev.sanmer.pi.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.PackageInfoCompat.isSystemApp
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.hidden.compat.delegate.AppOpsManagerDelegate
import dev.sanmer.hidden.compat.delegate.AppOpsManagerDelegate.Mode.Companion.isAllowed
import dev.sanmer.hidden.compat.delegate.PackageInstallerDelegate
import dev.sanmer.pi.Compat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.navigation.graphs.AppsScreen
import dev.sanmer.pi.utils.extensions.appSetting
import dev.sanmer.pi.utils.extensions.viewPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel(), AppOpsManagerDelegate.AppOpsCallback {
    private val pmCompat get() = Compat.packageManager
    private val aom by lazy {
        AppOpsManagerDelegate(
            Compat.appOpsService
        )
    }

    private val packageName = getPackageName(savedStateHandle)
    private val packageInfoInner get() = pmCompat.getPackageInfo(
        packageName, PackageManager.GET_PERMISSIONS, UserHandleCompat.myUserId()
    ).toIPackageInfo()

    private val packageInfoFlow = MutableStateFlow(packageInfoInner)
    var packageInfo by mutableStateOf(packageInfoFlow.value)
        private set
    var isRequester by mutableStateOf(false)
        private set
    var isExecutor by mutableStateOf(false)
        private set

    val appOps by lazy {
        AppOps(
            packageInfo = packageInfo,
            refresh = { packageInfoFlow.value = packageInfoInner }
        )
    }

    var opInstallPackageAllowed by mutableStateOf(packageInfo.isAuthorized())
        private set

    val hasOpInstallPackage by lazy {
        aom.getOpsForPackage(packageInfo).map { it.op }
            .contains(AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES)
    }

    override fun opChanged(op: Int, uid: Int, packageName: String) {
        opInstallPackageAllowed = aom.checkOpNoThrow(op, uid, packageName).isAllowed()
    }

    init {
        Timber.d("AppViewModel init")
        dataObserver()
        opInstallPackageObserver()
    }

    private fun dataObserver() {
        combine(
            userPreferencesRepository.data,
            packageInfoFlow
        ) { preferences, pi ->
            packageInfo = pi
            isRequester = preferences.requester == packageName
            isExecutor = preferences.executor == packageName

        }.launchIn(viewModelScope)
    }

    private fun opInstallPackageObserver() {
        aom.startWatchingMode(
            op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
            packageName = packageInfo.packageName,
            callback = this
        )

        addCloseable {
            aom.stopWatchingMode(callback = this)
        }
    }

    fun toggleOpInstallPackage(isAllowed: Boolean) {
        val setMode: (AppOpsManagerDelegate.Mode) -> Unit = {
            aom.setMode(
                op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
                packageInfo = packageInfo,
                mode = it
            )
        }

        when {
            isAllowed -> setMode(AppOpsManagerDelegate.Mode.Allow)
            else -> setMode(AppOpsManagerDelegate.Mode.Ignore)
        }
    }

    fun toggleRequester(current: Boolean) {
        if (!current) return

        viewModelScope.launch {
            userPreferencesRepository.setRequester(packageName)
        }
    }

    fun toggleExecutor(current: Boolean) {
        if (!current) return

        viewModelScope.launch {
            userPreferencesRepository.setExecutor(packageName)
        }
    }

    private fun PackageInfo.isAuthorized() = aom.checkOpNoThrow(
        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
        packageInfo = this
    ).isAllowed()

    class AppOps(
        private val packageInfo: IPackageInfo,
        private val refresh: () -> Unit
    ) {
        private val pmCompat get() = Compat.packageManager
        private val delegate by lazy {
            PackageInstallerDelegate(
                pmCompat.packageInstaller
            )
        }

        private val launchIntent by lazy {
            pmCompat.getLaunchIntentForPackage(
                packageInfo.packageName, UserHandleCompat.myUserId()
            )
        }

        val isOpenable by lazy { launchIntent != null }

        val isUninstallable by lazy {
            val isUpdatedSystemApp = packageInfo.applicationInfo.flags and
                    ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0

            when {
                isUpdatedSystemApp -> true
                else -> !packageInfo.isSystemApp
            }
        }

        fun launch(context: Context) {
            context.startActivity(launchIntent)
        }

        fun view(context: Context) {
            context.viewPackage(packageInfo.packageName)
        }

        fun setting(context: Context) {
            context.appSetting(packageInfo.packageName)
        }

        suspend fun uninstall() = withContext(Dispatchers.IO) {
            val result = delegate.uninstall(packageInfo.packageName)
            val status = result.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )

            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    Timber.i("Uninstall succeeded: packageName = ${packageInfo.packageName}")
                    if (packageInfo.isSystemApp) refresh()
                    return@withContext !packageInfo.isSystemApp
                }
                else -> {
                    val msg = result.getStringExtra(
                        PackageInstaller.EXTRA_STATUS_MESSAGE
                    )

                    Timber.e("Uninstall failed: packageName = ${packageInfo.packageName}, msg = $msg")
                    return@withContext false
                }
            }
        }

        suspend fun export(context: Context) = withContext(Dispatchers.IO) {
            val cr = context.contentResolver
            val downloadPath = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).let {
                File(it, "PI").apply { if (!exists()) mkdirs() }
            }

            val filename = with(packageInfo) { "${appLabel}-${versionName}-${longVersionCode}" }
            val sourceDir = File(packageInfo.applicationInfo.sourceDir)

            val files = sourceDir.parentFile
                ?.listFiles { _, name ->
                    name.endsWith(".apk")
                } ?: return@withContext false

            val streams = files.map { it to it.inputStream().buffered() }
            when {
                streams.size == 1 -> {
                    val apk = File(downloadPath, "${filename}.apk")
                    cr.openOutputStream(apk.toUri())?.use { output ->
                        streams.first().second.use { input ->
                            input.copyTo(output)
                        }
                    }

                    Timber.i("Export to ${apk.path}")
                }
                streams.size > 1 -> {
                    val apks = File(downloadPath, "${filename}.apks")
                    cr.openOutputStream(apks.toUri())?.let(::ZipOutputStream)?.use { output ->
                        streams.forEach { (file, input) ->
                            val entry = ZipEntry(file.name)
                            output.putNextEntry(entry)
                            input.copyTo(output)
                            input.close()
                            output.closeEntry()
                        }
                    }

                    Timber.i("Export to ${apks.path}")
                }
            }

            return@withContext true
        }
    }

    companion object {
        fun putPackageName(packageName: String) =
            AppsScreen.Details.route.replace(
                "{packageName}", packageName
            )

        fun getPackageName(savedStateHandle: SavedStateHandle): String =
            checkNotNull(savedStateHandle["packageName"])
    }
}