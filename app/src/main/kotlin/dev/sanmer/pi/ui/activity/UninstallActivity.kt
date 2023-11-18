package dev.sanmer.pi.ui.activity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.sanmer.pi.app.utils.ShizukuUtils
import dev.sanmer.pi.compat.ActivityMangerCompat
import dev.sanmer.pi.compat.PackageManagerCompat
import timber.log.Timber

@AndroidEntryPoint
class UninstallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("UninstallActivity onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initPackage(intent)

        finish()
    }

    override fun onDestroy() {
        Timber.i("UninstallActivity onDestroy")
        setResult(Activity.RESULT_OK)
        super.onDestroy()
    }

    private fun initPackage(intent: Intent?) {
        if (!ShizukuUtils.isEnable) {
            Timber.w("Shizuku not running")
            finish()
            return
        }

        val packageUri = intent?.data
        if (packageUri == null) {
            Timber.i("Failed to get packageUri")
            finish()
            return
        }

        val callingUid = ActivityMangerCompat.getCallingUid()
        val sourceInfo = getSourceInfo(callingUid)

        val packageName = packageUri.encodedSchemeSpecificPart
        Timber.d("From ${sourceInfo?.packageName}")
        Timber.d("Uninstall $packageName")
    }

    private fun getSourceInfo(callingUid: Int): PackageInfo? {
        return try {
            val packageName = PackageManagerCompat.getPackagesForUid(callingUid)
                .firstOrNull() ?: return null

            return PackageManagerCompat.getPackageInfo(packageName, 0, 0)
        } catch (ex: PackageManager.NameNotFoundException) {
            null
        }
    }
}