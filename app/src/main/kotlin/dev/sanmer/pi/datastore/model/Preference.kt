package dev.sanmer.pi.datastore.model

import dev.sanmer.pi.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class Preference(
    @ProtoNumber(1)
    val provider: Provider = Provider.None,
    @ProtoNumber(3)
    val requester: String = BuildConfig.APPLICATION_ID,
    @ProtoNumber(4)
    val executor: String = BuildConfig.APPLICATION_ID
) {
    fun encodeToStream(output: OutputStream) = output.write(
        ProtoBuf.encodeToByteArray(this)
    )

    companion object {
        fun decodeFromStream(input: InputStream): Preference =
            ProtoBuf.decodeFromByteArray(input.readBytes())
    }
}