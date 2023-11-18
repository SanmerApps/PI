package dev.sanmer.pi.repository

import android.content.pm.PackageInfo
import dev.sanmer.pi.database.dao.PackageDao
import dev.sanmer.pi.database.entity.PackageInfoEntity
import dev.sanmer.pi.model.IPackageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LocalRepository @Inject constructor(
    private val packageDao: PackageDao
) {
    fun getAllAsFlow() = packageDao.getAllAsFlow()

    fun getAuthorizedAllAsFlow() = packageDao.getAuthorizedAllAsFlow()

    suspend fun getAll() = withContext(Dispatchers.IO) {
        packageDao.getAll()
    }

    suspend fun getByPackageInfo(value: PackageInfo?) = withContext(Dispatchers.IO) {
        if (value == null) return@withContext false
        packageDao.getByPackageNameOrNull(value.packageName)?.authorized ?: false
    }

    suspend fun insert(value: IPackageInfo) = withContext(Dispatchers.IO) {
        val pie = PackageInfoEntity(value)
        packageDao.insert(pie)
    }

    suspend fun delete(values: List<PackageInfoEntity>) = withContext(Dispatchers.IO) {
        packageDao.delete(values)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        packageDao.deleteAll()
    }
}