package dev.sanmer.su

import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.shizuku.ShizukuBinderWrapper
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import rikka.shizuku.Shizuku as Raw

object Shizuku {
    class Wrapper internal constructor() : BinderWrapper {
        override fun wrap(original: IBinder): IBinder = ShizukuBinderWrapper(original)
    }

    suspend fun launch() = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            if (Raw.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                return@suspendCancellableCoroutine continuation.resume(Wrapper())
            }
            val listener = object : Raw.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(
                    requestCode: Int,
                    grantResult: Int
                ) {
                    Raw.removeRequestPermissionResultListener(this)
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        continuation.resume(Wrapper())
                    } else {
                        continuation.resumeWithException(IllegalStateException("Shizuku rejected"))
                    }
                }
            }
            Raw.addRequestPermissionResultListener(listener)
            Raw.requestPermission(listener.hashCode())
            continuation.invokeOnCancellation {
                Raw.removeRequestPermissionResultListener(listener)
            }
        }
    }
}