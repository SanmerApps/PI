package dev.sanmer.pi.app.utils

import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import rikka.shizuku.Shizuku

object ShizukuUtils: Shizuku.OnRequestPermissionResultListener {
    var isGranted by mutableStateOf(false)
        private set

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        isGranted = grantResult == PackageManager.PERMISSION_GRANTED
        if (isGranted) {
            Shizuku.removeRequestPermissionResultListener(this)
        }
    }

    init {
        if (isAlive) {
            isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermission() {
        if (isAlive && !isGranted) {
            Shizuku.addRequestPermissionResultListener(this)
            Shizuku.requestPermission(0)
        }
    }

    val isAlive get() = Shizuku.pingBinder()
    val atLeast12 get() = Shizuku.getVersion() >= 12
    val isEnable get() = isAlive && isGranted && atLeast12

    val isRoot get() = Shizuku.getUid() == 0
    val isAdb get() = Shizuku.getUid() == 2000
    val version get() = "API ${Shizuku.getVersion()}, " + when {
        isRoot -> "root"
        isAdb -> "adb"
        else -> IllegalStateException()
    }
}