package dev.sanmer.pi.repository

import androidx.datastore.core.DataStore
import dev.sanmer.pi.datastore.model.DarkMode
import dev.sanmer.pi.datastore.model.Preference
import dev.sanmer.pi.datastore.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferenceRepositoryImpl(
    private val dataStore: DataStore<Preference>
) : PreferenceRepository {
    override val data = dataStore.data

    override suspend fun setProvider(value: Provider) {
        withContext(Dispatchers.IO) {
            dataStore.updateData {
                it.copy(
                    provider = value
                )
            }
        }
    }

    override suspend fun setRequester(value: String) {
        withContext(Dispatchers.IO) {
            dataStore.updateData {
                it.copy(
                    requester = value
                )
            }
        }
    }

    override suspend fun setExecutor(value: String) {
        withContext(Dispatchers.IO) {
            dataStore.updateData {
                it.copy(
                    executor = value
                )
            }
        }
    }

    override suspend fun setDarkMode(value: DarkMode) {
        withContext(Dispatchers.IO) {
            dataStore.updateData {
                it.copy(
                    darkMode = value
                )
            }
        }
    }
}