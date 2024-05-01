package dev.sanmer.pi.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.hidden.compat.ContextCompat.userId
import dev.sanmer.hidden.compat.PackageInfoCompat.isSystemApp
import dev.sanmer.hidden.compat.UserHandleCompat
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.compat.ProviderCompat
import dev.sanmer.pi.model.IPackageInfo.Companion.toIPackageInfo
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.repository.UserPreferencesRepository
import dev.sanmer.pi.ui.navigation.graphs.AppsScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val localRepository: LocalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle,
    application: Application
) : AndroidViewModel(application) {
    private val context: Context by lazy { getApplication() }
    private val pmCompat get() = ProviderCompat.packageManagerCompat

    private val packageName = getPackageName(savedStateHandle)
    private val packageInfoInner by lazy {
        pmCompat.getPackageInfo(
            packageName, PackageManager.GET_PERMISSIONS, context.userId
        ).toIPackageInfo()
    }

    var packageInfo by mutableStateOf(packageInfoInner)
        private set

    val appOps by lazy {
        AppOps(context = context, packageInfo = packageInfo)
    }

    init {
        Timber.d("AppViewModel init")
        dataObserver()
    }

    private fun dataObserver() {
        combine(
            localRepository.getAllAsFlow(),
            userPreferencesRepository.data
        ) { authorized, preferences ->

            val isAuthorized = authorized.find {
                it.packageName == packageName
            }?.authorized ?: false

            packageInfo = packageInfoInner.copy(
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
                if (packageName != BuildConfig.APPLICATION_ID) {
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
                if (packageName != BuildConfig.APPLICATION_ID) {
                    userPreferencesRepository.setExecutor(BuildConfig.APPLICATION_ID)
                }
            }
            else -> {
                userPreferencesRepository.setExecutor(packageName)
            }
        }
    }

    class AppOps(
        private val context: Context,
        private val packageInfo: PackageInfo
    ) {
        private val isSelf = context.packageName == packageInfo.packageName
        private val pmCompat by lazy { ProviderCompat.packageManagerCompat }
        private val launchIntent by lazy {
            pmCompat.getLaunchIntentForPackage(
                packageInfo.packageName, UserHandleCompat.myUserId()
            )
        }

        val isOpenable get() = !isSelf && launchIntent != null
        val isUninstallable get() = !isSelf && !packageInfo.isSystemApp

        fun launch() {
            context.startActivity(launchIntent)
        }

        fun uninstall() {

        }

        fun export() {

        }
    }

    companion object {
        fun putPackageName(packageName: String) =
            AppsScreen.View.route.replace(
                "{packageName}", packageName
            )

        fun getPackageName(savedStateHandle: SavedStateHandle): String =
            checkNotNull(savedStateHandle["packageName"])
    }
}