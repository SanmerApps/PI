package dev.sanmer.pi.viewmodel

import android.Manifest
import android.content.Context
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
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.R
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.navigation.graphs.AppsScreen
import dev.sanmer.pi.utils.extensions.appSetting
import dev.sanmer.pi.utils.extensions.viewPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    private val delegate by lazy {
        AppOpsManagerDelegate(
            ProviderCompat.appOpsService
        )
    }

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

    private val opInstallPackage by lazy {
        delegate.opPermission(
            op = AppOpsManagerDelegate.OP_REQUEST_INSTALL_PACKAGES,
            uid = packageInfo.applicationInfo.uid,
            packageName = packageName
        )
    }
    var opInstallPackageAllowed by mutableStateOf(false)
        private set

    val hasOpInstallPackage by lazy {
        packageInfo.requestedPermissions?.contains(
            Manifest.permission.REQUEST_INSTALL_PACKAGES
        ) ?: false
    }

    private var isDefaultRequester by mutableStateOf(false)
    private var isDefaultExecutor by mutableStateOf(false)

    init {
        Timber.d("AppViewModel init")
        dataObserver()
        opInstallPackageObserver()
    }

    private fun dataObserver() {
        combine(
            localRepository.getPackageAuthorizedAllAsFlow(),
            userPreferencesRepository.data,
            packageInfoFlow
        ) { authorized, preferences, pi ->
            packageInfo = pi.copy(
                isAuthorized = authorized.contains(packageName),
                isRequester = preferences.requester == packageName,
                isExecutor = preferences.executor == packageName
            )

            isDefaultRequester = preferences.requester == BuildConfig.APPLICATION_ID
            isDefaultExecutor = preferences.executor == BuildConfig.APPLICATION_ID

        }.launchIn(viewModelScope)
    }

    private fun opInstallPackageObserver() {
        opInstallPackage.modeFlow
            .onEach {
                opInstallPackageAllowed = it.isAllowed()

            }.launchIn(viewModelScope)
    }

    fun toggleOpInstallPackage(isAllowed: Boolean) {
        when {
            isAllowed -> opInstallPackage.ignore()
            else -> opInstallPackage.allow()
        }
    }

    fun toggleAuthorized(value: Boolean) {
        viewModelScope.launch {
            localRepository.insertPackage(
                value = with(packageInfo) {
                    copy(isAuthorized = value)
                }
            )
        }
    }

    private fun setRequester(default: Boolean) {
        when {
            default -> userPreferencesRepository.setRequester(BuildConfig.APPLICATION_ID)
            else -> userPreferencesRepository.setRequester(packageName)
        }
    }

    fun requesterSelectableOps(context: Context) = buildList {
        add(object : SelectableOp {
            override val text: String = context.getString(R.string.details_ops_default)
            override val selected: Boolean = isDefaultRequester
            override fun onClick() = setRequester(true)
        })

        add(object : SelectableOp {
            override val text: String = context.getString(R.string.details_ops_this)
            override val selected: Boolean = packageInfo.isRequester
            override fun onClick() = setRequester(false)
        })
    }

    fun setExecutor(default: Boolean) {
        when {
            default -> userPreferencesRepository.setExecutor(BuildConfig.APPLICATION_ID)
            else -> userPreferencesRepository.setExecutor(packageName)
        }
    }

    fun executorSelectableOps(context: Context) = buildList {
        add(object : SelectableOp {
            override val text: String = context.getString(R.string.details_ops_default)
            override val selected: Boolean = isDefaultExecutor
            override fun onClick() = setExecutor(true)
        })

        add(object : SelectableOp {
            override val text: String = context.getString(R.string.details_ops_this)
            override val selected: Boolean = packageInfo.isExecutor
            override fun onClick() = setExecutor(false)
        })
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

        val isOpenable get() = launchIntent != null
        val isUninstallable get() = true

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
            context.appSetting(packageInfo.packageName)
        }
    }

    interface SelectableOp {
        val text: String
        val selected: Boolean
        fun onClick()
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