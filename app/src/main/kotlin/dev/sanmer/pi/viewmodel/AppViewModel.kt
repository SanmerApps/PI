package dev.sanmer.pi.viewmodel

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
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
import dev.sanmer.hidden.compat.delegate.PackageInstallerDelegate
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.navigation.graphs.AppsScreen
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
    private val localRepository: LocalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val pmCompat get() = ProviderCompat.packageManager

    private val packageName = getPackageName(savedStateHandle)
    private val packageInfoInner get() = pmCompat.getPackageInfo(
        packageName, PackageManager.GET_PERMISSIONS, UserHandleCompat.myUserId()
    ).toIPackageInfo()

    private val packageInfoFlow = MutableStateFlow(packageInfoInner)
    var packageInfo by mutableStateOf(packageInfoFlow.value)
        private set

    val appOps by lazy {
        AppOps(
            packageInfo = packageInfo,
            refresh = { packageInfoFlow.value = packageInfoInner }
        )
    }

    init {
        Timber.d("AppViewModel init")
        dataObserver()
    }

    private fun dataObserver() {
        combine(
            localRepository.getAllAsFlow(),
            userPreferencesRepository.data,
            packageInfoFlow
        ) { authorized, preferences, pi ->

            val isAuthorized = authorized.find {
                it.packageName == packageName
            }?.authorized ?: false

            packageInfo = pi.copy(
                isAuthorized = isAuthorized,
                isRequester = preferences.requester == packageName,
                isExecutor = preferences.executor == packageName
            )

        }.launchIn(viewModelScope)
    }

    fun toggleAuthorized() {
        viewModelScope.launch {
            localRepository.insert(
                value = with(packageInfo) {
                    copy(isAuthorized = !isAuthorized)
                }
            )
        }
    }

    fun toggleRequester() {
        when {
            packageInfo.isRequester -> {
                if (!packageInfo.isSelf) {
                    userPreferencesRepository.setRequester(BuildConfig.APPLICATION_ID)
                }
            }
            else -> {
                userPreferencesRepository.setRequester(packageName)
            }
        }
    }

    fun toggleExecutor() {
        when {
            packageInfo.isExecutor -> {
                if (!packageInfo.isSelf) {
                    userPreferencesRepository.setExecutor(BuildConfig.APPLICATION_ID)
                }
            }
            else -> {
                userPreferencesRepository.setExecutor(packageName)
            }
        }
    }

    class AppOps(
        private val packageInfo: IPackageInfo,
        private val refresh: () -> Unit
    ) {
        private val pmCompat get() = ProviderCompat.packageManager
        private val delegate by lazy {
            PackageInstallerDelegate(
                pmCompat.packageInstallerCompat
            )
        }

        private val launchIntent by lazy {
            pmCompat.getLaunchIntentForPackage(
                packageInfo.packageName, UserHandleCompat.myUserId()
            )
        }

        val isOpenable get() = !packageInfo.isSelf && launchIntent != null
        val isUninstallable get() = !packageInfo.isSelf

        fun launch(context: Context) {
            context.startActivity(launchIntent)
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

        fun view(context: Context) {
            context.viewPackage(packageInfo.packageName)
        }

        fun setting(context: Context) {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageInfo.packageName, null)
            )
            context.startActivity(intent)
        }
    }

    companion object {
        internal val IPackageInfo.isSelf
            get() = packageName == BuildConfig.APPLICATION_ID

        fun putPackageName(packageName: String) =
            AppsScreen.Details.route.replace(
                "{packageName}", packageName
            )

        fun getPackageName(savedStateHandle: SavedStateHandle): String =
            checkNotNull(savedStateHandle["packageName"])
    }
}