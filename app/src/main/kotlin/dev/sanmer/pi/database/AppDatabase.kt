package dev.sanmer.pi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.sanmer.pi.database.dao.PackageDao
import dev.sanmer.pi.database.entity.PackageInfoEntity

@Database(entities = [PackageInfoEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packageDao(): PackageDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "pi")
                .build()
        }
    }
}