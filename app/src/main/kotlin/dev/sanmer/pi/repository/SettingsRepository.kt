package dev.sanmer.pi.repository

import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.app.Const
import dev.sanmer.pi.database.dao.SettingDao
import dev.sanmer.pi.database.entity.SettingEntity
import kotlinx.coroutines.Dispatchers
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

    suspend fun getRequesterOrDefault() =
        settingDao.getByKey(Const.Settings.REQUESTER_PACKAGE_NAME)
            ?: BuildConfig.APPLICATION_ID

    suspend fun setRequester(value: String) = withContext(Dispatchers.IO) {
        insert(Const.Settings.REQUESTER_PACKAGE_NAME, value)
    }

    suspend fun getExecutorOrDefault() =
        settingDao.getByKey(Const.Settings.EXECUTOR_PACKAGE_NAME)
            ?: BuildConfig.APPLICATION_ID

    suspend fun setExecutor(value: String) = withContext(Dispatchers.IO) {
        insert(Const.Settings.EXECUTOR_PACKAGE_NAME, value)
    }
}