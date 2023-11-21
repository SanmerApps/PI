package dev.sanmer.pi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import dev.sanmer.pi.database.dao.PackageDao
import dev.sanmer.pi.database.dao.SettingDao
import dev.sanmer.pi.database.entity.PackageInfoEntity
import dev.sanmer.pi.database.entity.SettingEntity

@Database(entities = [PackageInfoEntity::class,SettingEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packageDao(): PackageDao
    abstract fun settingDao(): SettingDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "pi")
                .addMigrations(
                    MIGRATION_1_2
                )
                .build()
        }

        private val MIGRATION_1_2 = Migration(1, 2) {
            it.execSQL("CREATE TABLE IF NOT EXISTS settings (" +
                    "key TEXT NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "PRIMARY KEY(key))")
        }
    }
}