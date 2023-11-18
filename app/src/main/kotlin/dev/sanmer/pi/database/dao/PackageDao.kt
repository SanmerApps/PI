package dev.sanmer.pi.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sanmer.pi.database.entity.PackageInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages")
    fun getAllAsFlow(): Flow<List<PackageInfoEntity>>

    @Query("SELECT * FROM packages WHERE authorized = 1")
    fun getAuthorizedAllAsFlow(): Flow<List<PackageInfoEntity>>

    @Query("SELECT * FROM packages")
    fun getAll(): List<PackageInfoEntity>

    @Query("SELECT * FROM packages WHERE packageName = :packageName LIMIT 1")
    fun getByPackageNameOrNull(packageName: String): PackageInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: PackageInfoEntity)

    @Delete
    suspend fun delete(values: List<PackageInfoEntity>)

    @Query("DELETE FROM packages")
    suspend fun deleteAll()
}