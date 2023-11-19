package dev.sanmer.pi.viewmodel

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.app.Const
import dev.sanmer.pi.compat.PackageManagerCompat
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.ui.activity.InstallActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val localRepository: LocalRepository
) : ViewModel() {
    val authorized get() = localRepository.getAuthorizedAllAsFlow().map { it.size }
    var isPreferred by mutableStateOf(false)
        private set

    init {
        Timber.d("HomeViewModel init")
    }

    suspend fun getPreferred() {
        runCatching {
            val activities = PackageManagerCompat.getPreferredActivities(BuildConfig.APPLICATION_ID)
                .map { it.first.className }

            isPreferred = InstallActivity::class.java.name in activities
        }.onFailure {
            Timber.e(it, "getPreferredActivities")
        }
    }

    fun togglePreferred() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .setDataAndType(
                        Uri.parse("content://null"),
                        Const.MIME.APK
                    )

                @Suppress("DEPRECATION")
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_VIEW)
                    addAction(Intent.ACTION_INSTALL_PACKAGE)
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addDataScheme(ContentResolver.SCHEME_CONTENT)
                    addDataType(Const.MIME.APK)
                }

                var bestMatch = 0
                val set = PackageManagerCompat.queryIntentActivities(
                    intent,
                    Const.MIME.APK,
                    PackageManager.MATCH_DEFAULT_ONLY,
                    0
                ).map {
                    if (it.match > bestMatch) bestMatch = it.match

                    val packageName = it.activityInfo.packageName
                    val className = it.activityInfo.name

                    PackageManagerCompat.clearPackagePreferredActivities(packageName)
                    ComponentName(packageName, className)
                }.toTypedArray()

                if (isPreferred) return@runCatching

                val activity = ComponentName(
                    BuildConfig.APPLICATION_ID,
                    InstallActivity::class.java.name
                )

                PackageManagerCompat.addPreferredActivity(
                    filter, bestMatch, set, activity, 0
                )
            }.onFailure {
                Timber.e(it)
            }.onSuccess {
                getPreferred()
            }
        }
    }
}