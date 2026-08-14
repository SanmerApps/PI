package dev.sanmer.pi.model

sealed class LoadData<out V> {
    data object Pending : LoadData<Nothing>()
    data object Loading : LoadData<Nothing>()
    data class Success<out V>(val value: V) : LoadData<V>()
    data class Failure(val error: Throwable) : LoadData<Nothing>()

    val isPending inline get() = this == Pending
    val isLoading inline get() = this == Loading
    val isSuccess inline get() = this is Success
    val isFailure inline get() = this is Failure

    fun getOrThrow(): V {
        return when (this) {
            Pending -> throw IllegalStateException("Pending")
            Loading -> throw IllegalStateException("Loading")
            is Success<V> -> value
            is Failure -> throw error
        }
    }

    inline fun <R> getOrElse(transform: (V) -> R?, defaultValue: () -> R): R {
        return (this as? Success)?.value?.let(transform) ?: defaultValue()
    }

    inline fun onPending(action: () -> Unit): LoadData<V> {
        if (this == Pending) action()
        return this
    }

    inline fun onLoading(action: () -> Unit): LoadData<V> {
        if (this == Loading) action()
        return this
    }

    inline fun onSuccess(action: (V) -> Unit): LoadData<V> {
        (this as? Success)?.value?.let(action)
        return this
    }

    inline fun onFailure(action: (Throwable) -> Unit): LoadData<V> {
        (this as? Failure)?.error?.let(action)
        return this
    }

    companion object Default {
        inline fun <T, R> T.loadData(block: T.() -> R): LoadData<R> {
            return try {
                Success(block())
            } catch (e: Throwable) {
                Failure(e)
            }
        }
    }
}
