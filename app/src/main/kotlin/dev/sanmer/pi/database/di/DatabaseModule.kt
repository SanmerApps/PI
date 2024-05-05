package dev.sanmer.pi.database.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sanmer.pi.database.AppDatabase
import dev.sanmer.pi.database.dao.PackageDao
import dev.sanmer.pi.database.dao.SessionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providesAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.build(context)

    @Provides
    @Singleton
    fun providesPackageDao(db: AppDatabase): PackageDao = db.packageDao()

    @Provides
    @Singleton
    fun providesSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
}