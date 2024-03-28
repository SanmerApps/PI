package dev.sanmer.pi.datastore

import androidx.datastore.core.DataStore
import dev.sanmer.pi.datastore.UserPreferencesExt.Companion.new
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>
) {
    val data get() = userPreferences.data.map { UserPreferencesExt(it) }

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

    suspend fun setSelfUpdate(value: Boolean) = withContext(Dispatchers.IO) {
        userPreferences.updateData {
            it.new {
                selfUpdate = value
            }
        }
    }
}