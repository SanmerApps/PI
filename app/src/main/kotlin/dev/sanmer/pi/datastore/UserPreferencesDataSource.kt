package dev.sanmer.pi.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>
) {
    val data get() = userPreferences.data.map { it.toExt() }

    suspend fun setProvider(value: Provider) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.new {
                provider = value
            }
        }
    }

    suspend fun setDynamicColor(value: Boolean) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.new {
                dynamicColor = value
            }
        }
    }

    suspend fun setRequester(value: String) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.new {
                requester = value
            }
        }
    }

    suspend fun setExecutor(value: String) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.new {
                executor = value
            }
        }
    }
}