package dev.sanmer.pi.model

sealed class ServiceState {
    data object Pending : ServiceState()
    data class Success(val uid: Int) : ServiceState()
    data class Failure(val error: Throwable) : ServiceState()

    val isPending inline get() = this == Pending
    val isSucceed inline get() = this is Success
    val isFailed inline get() = this is Failure
}