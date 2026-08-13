package dev.sanmer.su

import android.content.Context

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
            Dhizuku.launch(context)
        }.fallback {
            LibSu.launch(context)
        }.getOrThrow()
    }
}