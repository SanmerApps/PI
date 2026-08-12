package dev.sanmer.su

import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import rikka.shizuku.Shizuku as RikkaShizuku

object Shizuku {
    class Wrapper internal constructor() : BinderWrapper {
        override fun getUid(): Int {
            return RikkaShizuku.getUid()
        }

        override fun getSELinuxContext(): String {
            return RikkaShizuku.getSELinuxContext().orEmpty()
        }

        override fun wrap(original: IBinder): IBinder {
            return ShizukuBinderWrapper(original)
        }
    }

    suspend fun launch() = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            if (RikkaShizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                return@suspendCancellableCoroutine continuation.resume(Wrapper())
            }
            val listener = object : RikkaShizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(
                    requestCode: Int,
                    grantResult: Int
                ) {
                    RikkaShizuku.removeRequestPermissionResultListener(this)
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        continuation.resume(Wrapper())
                    } else {
                        continuation.resumeWithException(IllegalStateException("Shizuku rejected"))
                    }
                }
            }
            RikkaShizuku.addRequestPermissionResultListener(listener)
            continuation.invokeOnCancellation {
                RikkaShizuku.removeRequestPermissionResultListener(listener)
            }
            RikkaShizuku.requestPermission(listener.hashCode())
        }
    }
}