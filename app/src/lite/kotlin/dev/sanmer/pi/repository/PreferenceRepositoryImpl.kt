package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.model.Preference
import dev.sanmer.pi.datastore.model.Provider
import kotlinx.coroutines.flow.flowOf

class PreferenceRepositoryImpl() : PreferenceRepository {
    override val data = flowOf(Preference())

    override suspend fun setProvider(value: Provider) {}
    override suspend fun setAutomatic(value: Boolean) {}
    override suspend fun setRequester(value: String) {}
    override suspend fun setExecutor(value: String) {}
}