package dev.sanmer.pi.repository

import dev.sanmer.pi.datastore.Provider
import dev.sanmer.pi.datastore.UserPreferencesDataSource
import dev.sanmer.pi.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    val data get() = userPreferencesDataSource.data

    fun setProvider(value: Provider) = applicationScope.launch {
        userPreferencesDataSource.setProvider(value)
    }

    fun setDynamicColor(value: Boolean) = applicationScope.launch {
        userPreferencesDataSource.setDynamicColor(value)
    }

    fun setRequester(value: String) = applicationScope.launch {
        userPreferencesDataSource.setRequester(value)
    }

    fun setExecutor(value: String) = applicationScope.launch {
        userPreferencesDataSource.setExecutor(value)
    }

    fun setSelfUpdate(value: Boolean) = applicationScope.launch {
        userPreferencesDataSource.setSelfUpdate(value)
    }
}