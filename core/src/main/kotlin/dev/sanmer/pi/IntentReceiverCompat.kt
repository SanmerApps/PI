package dev.sanmer.pi

import android.content.IIntentReceiver
import android.content.IIntentSender
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSenderHidden
import android.os.Bundle
import android.os.IBinder
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object IntentReceiverCompat {
    class IIntentSenderDelegate(
        private val onSend: (Intent) -> Unit
    ) : IIntentSender.Stub() {
        override fun send(
            code: Int,
            intent: Intent,
            resolvedType: String?,
            whitelistToken: IBinder?,
            finishedReceiver: IIntentReceiver?,
            requiredPermission: String?,
            options: Bundle?
        ) {
            onSend(intent)
        }
    }

    fun delegate(onSend: (Intent) -> Unit): IntentSender {
        val original = IIntentSenderDelegate(onSend)
        return Refine.unsafeCast(IntentSenderHidden(original))
    }

    suspend fun onDelegate(
        register: (IntentSender) -> Unit
    ) = suspendCancellableCoroutine { continuation ->
        register(
            delegate { continuation.resume(it) }
        )
    }
}