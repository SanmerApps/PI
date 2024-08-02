package dev.sanmer.pi.ktx

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine

suspend fun <T1, T2, R> Flow<T1>.combineToLatest(
    other: Flow<T2>,
    transform: suspend (T1, T2) -> R
) = combine(other) { t1, t2 -> t1 to t2 }
    .collectLatest { (t1, t2) -> transform(t1, t2) }