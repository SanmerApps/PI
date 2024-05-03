package dev.sanmer.pi.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.sanmer.pi.database.entity.SessionInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions")
    fun getAllAsFlow(): Flow<List<SessionInfoEntity>>

    @Query("SELECT * FROM sessions")
    fun getAll(): List<SessionInfoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: SessionInfoEntity)

    @Delete
    suspend fun delete(values: List<SessionInfoEntity>)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}