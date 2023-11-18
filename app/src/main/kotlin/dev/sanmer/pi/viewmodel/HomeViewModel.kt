package dev.sanmer.pi.viewmodel

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.app.Const
import dev.sanmer.pi.compat.PackageManagerCompat
import dev.sanmer.pi.repository.LocalRepository
import dev.sanmer.pi.ui.activity.InstallActivity
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val localRepository: LocalRepository
) : ViewModel() {
    val authorized get() = localRepository.getAuthorizedAllAsFlow().map { it.size }

    fun setPreferred() {
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

            val activity = ComponentName(
                BuildConfig.APPLICATION_ID,
                InstallActivity::class.java.name
            )
            PackageManagerCompat.addPreferredActivity(
                filter, bestMatch, set, activity, 0
            )

        }.onFailure {
            Timber.e(it, "addPreferredActivity")
        }
    }

    fun clearPreferred() {
        runCatching {
            PackageManagerCompat.clearPackagePreferredActivities(
                BuildConfig.APPLICATION_ID
            )
        }.onFailure {
            Timber.d(it.message)
        }
    }
}