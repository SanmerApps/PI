package dev.sanmer.pi.datastore

import androidx.datastore.core.DataStore
import dev.sanmer.pi.datastore.model.Provider
import dev.sanmer.pi.datastore.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>
) {
    val data get() = userPreferences.data

    suspend fun setProvider(value: Provider) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                provider = value
            )
        }
    }

    suspend fun setRequester(value: String) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                requester = value
            )
        }
    }

    suspend fun setExecutor(value: String) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.copy(
                executor = value
            )
        }
    }
}