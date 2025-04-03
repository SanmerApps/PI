package dev.sanmer.pi.receiver

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sanmer.pi.repository.PreferenceRepository
import dev.sanmer.pi.repository.ServiceRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BroadcastReceiverEntryPoint {
    fun preferenceRepository(): PreferenceRepository
    fun serviceRepository(): ServiceRepository
}