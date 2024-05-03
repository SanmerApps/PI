package dev.sanmer.pi.repository

import android.content.pm.PackageInfo
import dev.sanmer.hidden.compat.PackageInfoCompat.isEmpty
import dev.sanmer.pi.database.dao.PackageDao
import dev.sanmer.pi.database.entity.PackageInfoEntity
import dev.sanmer.pi.model.IPackageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRepository @Inject constructor(
    private val packageDao: PackageDao
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
        val pie = PackageInfoEntity(value)
        packageDao.insert(pie)
    }

    suspend fun deletePackage(values: List<PackageInfoEntity>) = withContext(Dispatchers.IO) {
        packageDao.delete(values)
    }

    suspend fun deletePackageAll() = withContext(Dispatchers.IO) {
        packageDao.deleteAll()
    }
}