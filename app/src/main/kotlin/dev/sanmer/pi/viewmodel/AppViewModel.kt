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
import dev.sanmer.pi.Compat
import dev.sanmer.pi.PackageInfoCompat.isSystemApp
import dev.sanmer.pi.UserHandleCompat
import dev.sanmer.pi.compat.MediaStoreCompat.createDownloadUri
import dev.sanmer.pi.delegate.AppOpsManagerDelegate
import dev.sanmer.pi.delegate.AppOpsManagerDelegate.Mode.Companion.isAllowed
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
    private val pm by lazy { Compat.getPackageManager() }
    private val pi by lazy { Compat.getPackageInstaller() }
    private val aom by lazy { Compat.getAppOpsService() }

    private val packageName = getPackageName(savedStateHandle)
    private val packageInfoInner get() = pm.getPackageInfo(
        packageName, PackageManager.GET_PERMISSIONS, UserHandleCompat.myUserId()
    ).toIPackageInfo()

    private val packageInfoFlow = MutableStateFlow(packageInfoInner)
    var packageInfo by mutableStateOf(packageInfoFlow.value)
        private set
    var isRequester by mutableStateOf(false)
        private set
    var isExecutor by mutableStateOf(false)
        private set

    val settings by lazy { buildSettings() }

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

    private fun buildSettings() = object : Settings {
        private val launchIntent by lazy {
            pm.getLaunchIntentForPackage(
                packageInfo.packageName, UserHandleCompat.myUserId()
            )
        }

        override val isOpenable by lazy { launchIntent != null }

        override val isUninstallable by lazy {
            val isUpdatedSystemApp = packageInfo.applicationInfo?.let {
                it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
            } ?: false

            when {
                isUpdatedSystemApp -> true
                else -> !packageInfo.isSystemApp
            }
        }

        override fun launch(context: Context) {
            context.startActivity(launchIntent)
        }

        override fun view(context: Context) {
            context.viewPackage(packageInfo.packageName)
        }

        override fun setting(context: Context) {
            context.appSetting(packageInfo.packageName)
        }

        override suspend fun uninstall() = withContext(Dispatchers.IO) {
            val result = pi.uninstall(packageInfo.packageName)
            val status = result.getIntExtra(
                PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE
            )

            when (status) {
                PackageInstaller.STATUS_SUCCESS -> {
                    if (packageInfo.isSystemApp) {

                    }

                    !packageInfo.isSystemApp
                }
                else -> false
            }
        }

        override suspend fun export(context: Context): Boolean {
            val sourceDir = packageInfo.applicationInfo?.let { File(it.sourceDir) }
            if (sourceDir == null) return false

            val filename = with(packageInfo) { "${appLabel}-${versionName}-${longVersionCode}.apk" }
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

    private fun PackageInfo.isAuthorized() = aom.checkOpNoThrow(
        op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
        packageInfo = this
    ).isAllowed()

    interface Settings {
        val isOpenable: Boolean
        val isUninstallable: Boolean
        fun launch(context: Context)
        fun view(context: Context)
        fun setting(context: Context)
        suspend fun uninstall(): Boolean
        suspend fun export(context: Context): Boolean
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