package dev.sanmer.pi.repository

import androidx.datastore.core.DataStore
import dev.sanmer.pi.datastore.model.Preference

class PreferenceRepositoryImpl(
    private val dataStore: DataStore<Preference>
) : PreferenceRepository {
    override val data = dataStore.data

    override suspend fun setOriginatingPackageName(value: String) {
        dataStore.updateData {
            it.copy(originatingPackageName = if (it.originatingPackageName == value) "" else value)
        }
    }

    override suspend fun setInstallerPackageName(value: String) {
        dataStore.updateData {
            it.copy(installerPackageName = if (it.installerPackageName == value) "" else value)
        }
    }
}