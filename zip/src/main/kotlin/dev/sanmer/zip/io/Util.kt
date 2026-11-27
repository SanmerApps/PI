@file:Suppress("NOTHING_TO_INLINE")

package dev.sanmer.zip.io

internal inline fun checkBounds(size: Long, offset: Long, length: Long) {
    checkBounds(size, offset)
    if (length < 0 || length > size - offset) {
        throw IndexOutOfBoundsException("Expect length in 0..${size - offset}, but $length")
    }
}

internal inline fun checkBounds(size: Long, offset: Long) {
    if (offset !in 0..size) {
        throw IndexOutOfBoundsException("Expect offset in 0..${size}, but $offset")
    }
}

internal inline fun checkBounds(size: Int, offset: Int, length: Int) =
    checkBounds(size.toLong(), offset.toLong(), length.toLong())

internal inline fun checkBounds(size: Int, offset: Int) =
    checkBounds(size.toLong(), offset.toLong())

internal inline fun checkByteCount(byteCount: Long) {
    require(byteCount >= 0) { "Expect byteCount >= 0, but $$byteCount" }
}