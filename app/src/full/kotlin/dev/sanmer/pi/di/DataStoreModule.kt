package dev.sanmer.pi.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import dev.sanmer.pi.datastore.PreferenceSerializer
import dev.sanmer.pi.datastore.model.Preference
import org.koin.dsl.module

val DataStore = module {
    factory<Serializer<Preference>> {
        PreferenceSerializer()
    }

    factory<DataStore<Preference>> {
        DataStoreFactory.create(
            serializer = get()
        ) {
            get<Context>().dataStoreFile("user_preferences.pb")
        }
    }
}