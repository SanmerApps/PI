package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.model.Preference
import kotlinx.coroutines.flow.Flow

interface PreferenceRepository {
    val data: Flow<Preference>
    suspend fun setOriginatingPackageName(value: String)
    suspend fun setInstallerPackageName(value: String)
}