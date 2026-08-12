package dev.sanmer.pi.ktx

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

inline fun <reified T> Flow<T>.sampleIf(
    period: Duration,
    crossinline predicate: suspend (T) -> Boolean
): Flow<T> = flow {
    var lastAccepted: TimeMark? = null
    collect { value ->
        if (predicate(value)) {
            val last = lastAccepted
            if (last == null || last.elapsedNow() >= period) {
                lastAccepted = TimeSource.Monotonic.markNow()
                emit(value)
            }
        } else {
            emit(value)
        }
    }
}