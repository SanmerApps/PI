package dev.sanmer.su

import android.content.Context
import android.content.pm.PackageManager
import android.os.IBinder
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.rosan.dhizuku.api.Dhizuku as Raw

object Dhizuku {
    class Wrapper internal constructor() : BinderWrapper {
        override val ownerPackageName: String get() = Raw.getOwnerPackageName()
        override fun wrap(original: IBinder): IBinder = Raw.binderWrapper(original)
    }

    suspend fun launch(context: Context) = withContext(Dispatchers.Main) {
        check(Raw.init(context)) { "Dhizuku lost" }
        if (Raw.isPermissionGranted()) return@withContext Wrapper()

        suspendCancellableCoroutine { continuation ->
            val listener = object : DhizukuRequestPermissionListener() {
                override fun onRequestPermission(grantResult: Int) {
                    if (continuation.isCompleted) return
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        continuation.resume(Wrapper())
                    } else {
                        continuation.resumeWithException(IllegalStateException("Dhizuku rejected"))
                    }
                }
            }
            Raw.requestPermission(listener)
        }
    }
}