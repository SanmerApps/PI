package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.PreferenceDataSource
import dev.sanmer.pi.datastore.model.Provider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceRepository @Inject constructor(
    private val dataSource: PreferenceDataSource
) {
    val data get() = dataSource.data

    suspend fun setProvider(value: Provider) {
        dataSource.setProvider(value)
    }

    suspend fun setRequester(value: String) {
        dataSource.setRequester(value)
    }

    suspend fun setExecutor(value: String) {
        dataSource.setExecutor(value)
    }
}