package dev.sanmer.pi.di

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import dev.sanmer.pi.datastore.PreferenceSerializer
import dev.sanmer.pi.datastore.model.Preference
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory

val DataStoreModule = module {
    factory<PreferenceSerializer>() bind Serializer::class

    factory {
        DataStoreFactory.create(
            serializer = get<Serializer<Preference>>()
        ) {
            androidContext().dataStoreFile("preference.pb")
        }
    }
}