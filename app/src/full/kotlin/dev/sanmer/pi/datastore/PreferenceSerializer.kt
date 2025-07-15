package dev.sanmer.pi.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import dev.sanmer.pi.datastore.model.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

class PreferenceSerializer() : Serializer<Preference> {
    override val defaultValue = Preference()

    override suspend fun readFrom(input: InputStream) =
        try {
            ProtoBuf.decodeFromByteArray<Preference>(input.readBytes())
        } catch (e: SerializationException) {
            throw CorruptionException("Failed to read proto", e)
        }

    override suspend fun writeTo(t: Preference, output: OutputStream) =
        withContext(Dispatchers.IO) {
            output.write(ProtoBuf.encodeToByteArray(t))
        }
}