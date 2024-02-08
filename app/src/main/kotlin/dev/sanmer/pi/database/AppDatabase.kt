package dev.sanmer.pi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import dev.sanmer.pi.database.dao.PackageDao
import dev.sanmer.pi.database.entity.PackageInfoEntity

@Database(entities = [PackageInfoEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun packageDao(): PackageDao

    companion object {
        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "pi")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3
                )
                .build()
        }

        private val MIGRATION_1_2 = Migration(1, 2) {
            it.execSQL("CREATE TABLE IF NOT EXISTS settings (" +
                    "key TEXT NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "PRIMARY KEY(key))")
        }

        private val MIGRATION_2_3 = Migration(2, 3) {
            it.execSQL("DROP TABLE settings")
        }
    }
}