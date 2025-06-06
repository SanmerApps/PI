package kotlinx.serialization.protobuf

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule

object ProtoBuf : BinaryFormat {
    override val serializersModule: SerializersModule
        get() = throw UnsupportedOperationException()

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        throw UnsupportedOperationException()
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        throw UnsupportedOperationException()
    }
}