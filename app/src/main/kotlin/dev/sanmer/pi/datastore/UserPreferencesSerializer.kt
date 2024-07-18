package dev.sanmer.pi.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sanmer.pi.datastore.model.UserPreferences
import kotlinx.serialization.SerializationException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

class UserPreferencesSerializer @Inject constructor() : Serializer<UserPreferences> {
    override val defaultValue = UserPreferences()

    override suspend fun readFrom(input: InputStream) =
        try {
            UserPreferences.decodeFrom(input)
        } catch (e: SerializationException) {
            throw CorruptionException("Failed to read proto", e)
        }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        t.encodeTo(output)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object Provider {
        @Provides
        @Singleton
        fun DataStore(
            @ApplicationContext context: Context,
            userPreferencesSerializer: UserPreferencesSerializer
        ): DataStore<UserPreferences> =
            DataStoreFactory.create(
                serializer = userPreferencesSerializer
            ) {
                context.dataStoreFile("user_preferences.pb")
            }
    }
}