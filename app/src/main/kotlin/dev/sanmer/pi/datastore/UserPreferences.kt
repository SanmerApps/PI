package dev.sanmer.pi.datastore

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
    val provider: Provider,
    val dynamicColor: Boolean,
    val requester: String,
    val executor: String
) {
    fun writeTo(out: OutputStream) = out.write(
        ProtoBuf.encodeToByteArray(this)
    )

    companion object {
        fun from(input: InputStream): UserPreferences =
            ProtoBuf.decodeFromByteArray(
                input.readBytes()
            )

        fun default() = UserPreferences(
            provider = Provider.None,
            dynamicColor = BuildCompat.atLeastS,
            requester = BuildConfig.APPLICATION_ID,
            executor = BuildConfig.APPLICATION_ID
        )
    }
}