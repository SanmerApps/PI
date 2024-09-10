package dev.sanmer.pi.datastore

import androidx.datastore.core.DataStore
import dev.sanmer.pi.datastore.model.Preference
import dev.sanmer.pi.datastore.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PreferenceDataSource @Inject constructor(
    private val dataStore: DataStore<Preference>
) {
    val data get() = dataStore.data

    suspend fun setProvider(value: Provider) = withContext(Dispatchers.IO) {
        dataStore.updateData {
            it.copy(
                provider = value
            )
        }
    }

    suspend fun setRequester(value: String) = withContext(Dispatchers.IO) {
        dataStore.updateData {
            it.copy(
                requester = value
            )
        }
    }

    suspend fun setExecutor(value: String) = withContext(Dispatchers.IO) {
        dataStore.updateData {
            it.copy(
                executor = value
            )
        }
    }
}