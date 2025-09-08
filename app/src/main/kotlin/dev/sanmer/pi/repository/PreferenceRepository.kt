package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.model.DarkMode
import dev.sanmer.pi.datastore.model.Preference
import dev.sanmer.pi.datastore.model.Provider
import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    val data: Flow<Preference>
    suspend fun setProvider(value: Provider)
    suspend fun setRequester(value: String)
    suspend fun setExecutor(value: String)
    suspend fun setDarkMode(value: DarkMode)
}