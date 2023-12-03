package dev.sanmer.pi.repository

import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.app.Settings
import dev.sanmer.pi.database.dao.SettingDao
import dev.sanmer.pi.database.entity.SettingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingDao: SettingDao
) {
    private suspend fun insert(key: String, value: String) = withContext(Dispatchers.IO) {
        settingDao.insert(SettingEntity(key = key, value = value))
    }

    fun getWorkingModeOrNone() =
        settingDao.getByKeyAsFlow(Settings.MODE)
            .map {
                it?.let(Settings.Provider::valueOf)
                    ?: Settings.Provider.None
            }

    suspend fun setWorkingMode(value: Settings.Provider) =
        insert(Settings.MODE, value.name)

    suspend fun getRequesterOrDefault() =
        settingDao.getByKey(Settings.REQUESTER)
            ?: BuildConfig.APPLICATION_ID

    suspend fun setRequester(value: String) =
        insert(Settings.REQUESTER, value)

    suspend fun getExecutorOrDefault() =
        settingDao.getByKey(Settings.EXECUTOR)
            ?: BuildConfig.APPLICATION_ID

    suspend fun setExecutor(value: String) =
        insert(Settings.EXECUTOR, value)
}