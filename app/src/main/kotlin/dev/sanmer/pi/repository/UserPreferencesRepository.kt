package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.UserPreferencesDataSource
import dev.sanmer.pi.datastore.model.Provider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) {
    val data get() = userPreferencesDataSource.data

    suspend fun setProvider(value: Provider) = userPreferencesDataSource.setProvider(value)

    suspend fun setDynamicColor(value: Boolean) = userPreferencesDataSource.setDynamicColor(value)

    suspend fun setRequester(value: String) = userPreferencesDataSource.setRequester(value)

    suspend fun setExecutor(value: String) = userPreferencesDataSource.setExecutor(value)
}