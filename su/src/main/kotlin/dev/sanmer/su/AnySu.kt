package dev.sanmer.su

import android.content.Context
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

object AnySu {
    private inline fun <T> Result<T>.fallback(block: () -> T): Result<T> {
        return when (val exception = exceptionOrNull()) {
            null -> this
            else -> runCatching(block).onFailure { it.addSuppressed(exception) }
        }
    }

    suspend fun launch(context: Context): BinderWrapper {
        return runCatching {
            Shizuku.launch()
        }.fallback {
            withTimeout(10.seconds) {
                LibSu.launch(context)
            }
        }.getOrThrow()
    }
}