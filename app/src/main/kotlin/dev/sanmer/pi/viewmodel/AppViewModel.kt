package dev.sanmer.pi.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import dev.sanmer.pi.compat.MediaStoreCompat.createDownloadUri
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
import java.io.InputStream
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
        packageInfo.requestedPermissions?.contains(
            Manifest.permission.REQUEST_INSTALL_PACKAGES
        ) == true ||
                aom.getOpsForPackage(packageInfo).map { it.op }
            .contains(AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES)
    }

    override fun opChanged(op: Int, uid: Int, packageName: String) {
        opInstallPackageAllowed = aom.checkOpNoThrow(op, uid, packageName).isAllowed()
    }

    init {
        Timber.d("AppViewModel init: $packageName")
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
            else -> setMode(AppOpsManagerDelegate.Mode.Default)
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
                    if (packageInfo.isSystemApp) refresh()
                    !packageInfo.isSystemApp
                }
                else -> {
                    false
                }
            }
        }

        suspend fun export(context: Context): Boolean {
            val filename = with(packageInfo) { "${appLabel}-${versionName}-${longVersionCode}.apk" }
            val sourceDir = File(packageInfo.applicationInfo.sourceDir)
            val path = "PI" + File.separator + filename

            val files = sourceDir.parentFile?.listFiles { file ->
                file.name.endsWith(".apk")
            } ?: return false

            val streams = files.map { it to it.inputStream().buffered() }
            when {
                streams.size == 1 -> {
                    context.exportApk(
                        input = streams.first().second,
                        path = path
                    )
                }

                streams.size > 1 -> {
                    context.exportApks(
                        inputs = streams,
                        path = path + 's'
                    )
                }
            }

            streams.forEach { it.second.close() }
            return true
        }

        private suspend fun Context.exportApk(
            input: InputStream,
            path: String,
        ) = withContext(Dispatchers.IO) {
            val uri = createDownloadUri(
                path = path,
                mimeType = "android/vnd.android.package-archive"
            )

            contentResolver.openOutputStream(uri)?.use { output ->
                input.copyTo(output)
                return@withContext true
            }

            false
        }

        private suspend fun Context.exportApks(
            inputs: List<Pair<File, InputStream>>,
            path: String,
        ) = withContext(Dispatchers.IO) {
            val uri = createDownloadUri(
                path = path,
                mimeType = "android/zip"
            )

            contentResolver.openOutputStream(uri)?.let(::ZipOutputStream)?.use { output ->
                inputs.forEach { (file, input) ->
                    output.putNextEntry(ZipEntry(file.name))
                    input.copyTo(output)
                    output.closeEntry()
                }

                return@withContext true
            }

            false
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