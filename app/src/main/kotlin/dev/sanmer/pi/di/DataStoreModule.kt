package dev.sanmer.pi.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.dataStoreFile
import dev.sanmer.pi.datastore.PreferenceSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val DataStoreModule = module {
    single {
        DataStore.Builder(
            storage = FileStorage(
                serializer = PreferenceSerializer(),
                produceFile = { androidContext().dataStoreFile("preference.pb") }
            ),
            context = SupervisorJob() + Dispatchers.IO
        ).build()
    }
}