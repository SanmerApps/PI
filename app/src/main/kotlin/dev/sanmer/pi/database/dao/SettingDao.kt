package dev.sanmer.pi.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sanmer.pi.database.entity.SettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingDao {
    @Query("SELECT value FROM settings WHERE `key` = :key")
    fun getByKeyAsFlow(key: String): Flow<String>

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getByKey(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: SettingEntity)

    @Query("DELETE FROM settings")
    suspend fun deleteAll()
}