package dev.sanmer.pi.datastore.model

import dev.sanmer.pi.BuildConfig
import dev.sanmer.pi.compat.BuildCompat
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class UserPreferences(
    val provider: Provider = Provider.None,
    val dynamicColor: Boolean = BuildCompat.atLeastS,
    val requester: String = BuildConfig.APPLICATION_ID,
    val executor: String = BuildConfig.APPLICATION_ID
) {
    fun encodeTo(output: OutputStream) = output.write(
        ProtoBuf.encodeToByteArray(this)
    )

    companion object {
        fun decodeFrom(input: InputStream): UserPreferences =
            ProtoBuf.decodeFromByteArray(input.readBytes())
    }
}