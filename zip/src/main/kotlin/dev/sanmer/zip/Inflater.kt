package dev.sanmer.zip

import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative
import dev.sanmer.zip.io.checkBounds
import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations

class Inflater(bits: Int) {
    init {
        val ret = ReturnCode(Native.init(bits))
        check(ret == ReturnCode.Ok) { ret }
    }

    val availIn get() = Native.getAvailIn()
    val availOut get() = Native.getAvailOut()
    val totalIn get() = Native.getTotalIn()
    val totalOut get() = Native.getTotalOut()
    val dataType get() = Native.getDataType()

    fun prime(bits: Int, value: Int) = ReturnCode(
        Native.prime(bits, value)
    )

    fun setDictionary(bytes: ByteArray, offset: Int, length: Int): ReturnCode {
        checkBounds(bytes.size, offset, length)
        return ReturnCode(Native.setDictionary(bytes, offset, length))
    }

    fun setDictionary(bytes: ByteArray) = ReturnCode(
        Native.setDictionary(bytes, 0, bytes.size)
    )

    fun inflate(
        inBytes: ByteArray,
        inOffset: Int,
        inLength: Int,
        outBytes: ByteArray,
        outOffset: Int,
        outLength: Int,
        flush: Flush
    ): ReturnCode {
        checkBounds(inBytes.size, inOffset, inLength)
        checkBounds(outBytes.size, outOffset, outLength)
        return ReturnCode(
            Native.inflate(inBytes, inOffset, inLength, outBytes, outOffset, outLength, flush.value)
        )
    }

    @OptIn(UnsafeIoApi::class)
    fun inflate(inBuffer: Buffer, outBuffer: Buffer, flush: Flush): Pair<ReturnCode, Int> {
        val ret: ReturnCode
        val produced: Int
        UnsafeBufferOperations.writeToTail(outBuffer, 1) { outBytes, outPos, outLimit ->
            val outLength = outLimit - outPos
            UnsafeBufferOperations.readFromHead(inBuffer) { inBytes, inPos, inLimit ->
                val inLength = inLimit - inPos
                ret = inflate(
                    inBytes,
                    inPos,
                    inLength,
                    outBytes,
                    outPos,
                    outLength,
                    flush
                )
                inLength - availIn
            }
            produced = outLength - availOut
            produced
        }
        return ret to produced
    }

    @JvmInline
    value class ReturnCode(internal val value: Int) {
        override fun toString() = when (this) {
            Ok -> "Ok"
            StreamEnd -> "StreamEnd"
            NeedDict -> "NeedDict"
            ErrNo -> "ErrNo"
            StreamError -> "StreamError"
            DataError -> "DataError"
            MemError -> "MemError"
            BufError -> "BufError"
            VersionError -> "VersionError"
            else -> "ReturnCode($value)"
        }

        companion object Default {
            val Ok = ReturnCode(0)
            val StreamEnd = ReturnCode(1)
            val NeedDict = ReturnCode(2)
            val ErrNo = ReturnCode(-1)
            val StreamError = ReturnCode(-2)
            val DataError = ReturnCode(-3)
            val MemError = ReturnCode(-4)
            val BufError = ReturnCode(-5)
            val VersionError = ReturnCode(-6)
        }
    }

    enum class Flush(internal val value: Int) {
        NoFlush(0),
        SyncFlush(2),
        Finish(4),
        Block(5),
        Trees(6)
    }

    private object Native {
        init {
            System.loadLibrary("z_rs")
        }

        @CriticalNative
        @JvmStatic
        external fun getAvailIn(): Int

        @CriticalNative
        @JvmStatic
        external fun getAvailOut(): Int

        @CriticalNative
        @JvmStatic
        external fun getTotalIn(): Long

        @CriticalNative
        @JvmStatic
        external fun getTotalOut(): Long

        @CriticalNative
        @JvmStatic
        external fun getDataType(): Int

        @CriticalNative
        @JvmStatic
        external fun init(bits: Int): Int

        @CriticalNative
        @JvmStatic
        external fun prime(bits: Int, value: Int): Int

        @FastNative
        @JvmStatic
        external fun setDictionary(bytes: ByteArray, offset: Int, length: Int): Int

        @FastNative
        @JvmStatic
        external fun inflate(
            inBytes: ByteArray,
            inOffset: Int,
            inLength: Int,
            outBytes: ByteArray,
            outOffset: Int,
            outLength: Int,
            flush: Int,
        ): Int
    }

    companion object Default {
        const val MIN_WINDOW_BITS = 8
        const val MAX_WINDOW_BITS = 15
    }
}
