package dev.sanmer.pi.model

import dev.sanmer.su.IServiceManager

sealed class ServiceState {
    data object Pending : ServiceState()
    data class Success(val service: IServiceManager) : ServiceState()
    data class Failure(val error: Throwable) : ServiceState()

    val isPending inline get() = this == Pending
    val isSucceed inline get() = this is Success
    val isFailed inline get() = this is Failure
}