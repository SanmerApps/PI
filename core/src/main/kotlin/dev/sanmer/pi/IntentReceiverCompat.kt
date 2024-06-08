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
    suspend fun build(
        register: (IntentSender) -> Unit
    ) = suspendCancellableCoroutine { continuation ->
        val mLocalSender: IIntentSender.Stub = object : IIntentSender.Stub() {
            override fun send(
                code: Int,
                intent: Intent,
                resolvedType: String?,
                whitelistToken: IBinder?,
                finishedReceiver: IIntentReceiver?,
                requiredPermission: String?,
                options: Bundle?
            ) {
                continuation.resume(intent)
            }
        }

        val intentSender: IntentSender = Refine.unsafeCast(IntentSenderHidden(mLocalSender))
        register(intentSender)
    }
}