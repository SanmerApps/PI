package dev.sanmer.pi.model

sealed class ShizukuState {
    data object Pending : ShizukuState()
    data class Success(val uid: Int) : ShizukuState()
    data class Failure(val error: Throwable) : ShizukuState()

    val isPending inline get() = this == Pending
    val isSucceed inline get() = this is Success
    val isFailed inline get() = this is Failure
}