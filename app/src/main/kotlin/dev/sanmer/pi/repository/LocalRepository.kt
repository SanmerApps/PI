package dev.sanmer.pi.repository

import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import dev.sanmer.hidden.compat.PackageInfoCompat.isEmpty
import dev.sanmer.pi.database.dao.PackageDao
import dev.sanmer.pi.database.dao.SessionDao
import dev.sanmer.pi.database.entity.PackageInfoEntity
import dev.sanmer.pi.database.entity.SessionInfoEntity
import dev.sanmer.pi.model.IPackageInfo
import dev.sanmer.pi.model.ISessionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor(
    private val packageDao: PackageDao,
    private val sessionDao: SessionDao
) {
    fun getPackageAuthorizedAllAsFlow() = packageDao.getAuthorizedAllAsFlow()
        .map { list ->
            list.map { it.packageName }
        }

    suspend fun getPackageAll() = withContext(Dispatchers.IO) {
        packageDao.getAll()
    }

    suspend fun getByPackageInfo(value: PackageInfo) = withContext(Dispatchers.IO) {
        if (value.isEmpty) return@withContext false
        packageDao.getByPackageNameOrNull(value.packageName)?.authorized ?: false
    }

    suspend fun insertPackage(value: IPackageInfo) = withContext(Dispatchers.IO) {
        packageDao.insert(
            PackageInfoEntity(value)
        )
    }

    suspend fun deletePackage(values: List<PackageInfoEntity>) = withContext(Dispatchers.IO) {
        packageDao.delete(values)
    }

    fun getSessionAllAsFlow() = sessionDao.getAllAsFlow()
        .map { list ->
            list.map { it.toISessionInfo() }
        }

    suspend fun getSessionAll() = withContext(Dispatchers.IO) {
        sessionDao.getAll().map { it.toISessionInfo() }
    }

    suspend fun insertSession(value: PackageInstaller.SessionInfo) = withContext(Dispatchers.IO) {
        sessionDao.insert(
            SessionInfoEntity(value)
        )
    }

    suspend fun insertSession(value: ISessionInfo) = withContext(Dispatchers.IO) {
        sessionDao.insert(
            SessionInfoEntity(value)
        )
    }

    suspend fun deleteSessionAll() = withContext(Dispatchers.IO) {
        sessionDao.deleteAll()
    }
}