@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.errors.*
import org.khronos.webgl.*

fun Input.readFully(dst: Int8Array, offset: Int = 0, length: Int = dst.length - offset) {
    if (this is AbstractInput) {
        return readFully(dst, offset, length)
    }

    TODO_ERROR()
}

fun Input.readFully(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength - offset) {
    if (this is AbstractInput) {
        return readFully(dst, offset, length)
    }
    TODO_ERROR()
}

fun Input.readFully(dst: ArrayBufferView, byteOffset: Int = 0, byteLength: Int = dst.byteLength - byteOffset) {
    if (this is AbstractInput) {
        return readFully(dst, byteOffset, byteLength)
    }
    TODO_ERROR()
}

fun Input.readAvailable(dst: Int8Array, offset: Int = 0, length: Int = dst.length - offset): Int {
    if (this is AbstractInput) {
        return readAvailable(dst, offset, length)
    }
    TODO_ERROR()
}

fun Input.readAvailable(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength - offset): Int {
    if (this is AbstractInput) {
        return readAvailable(dst, offset, length)
    }
    TODO_ERROR()
}

fun Input.readAvailable(dst: ArrayBufferView, byteOffset: Int = 0, byteLength: Int = dst.byteLength - byteOffset): Int {
    if (this is AbstractInput) {
        return readAvailable(dst, byteOffset, byteLength)
    }
    TODO_ERROR()
}

internal fun AbstractInput.readFully(dst: Int8Array, offset: Int, length: Int) {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return readFully(dst as ArrayBufferView, offset, length)
}

internal fun AbstractInput.readFully(dst: ArrayBuffer, offset: Int, length: Int) {
    if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
    var copied = 0

    takeWhile { buffer: Buffer ->
        val rc = buffer.readAvailable(dst, offset + copied, length - copied)
        if (rc > 0) copied += rc
        copied < length
    }
}

internal fun AbstractInput.readFully(dst: ArrayBufferView, offset: Int, length: Int) {
    require(length <= dst.byteLength) {
        throw IndexOutOfBoundsException("length $length is greater than view size ${dst.byteLength}")
    }

    return readFully(dst.buffer, dst.byteOffset + offset, length)
}

internal fun AbstractInput.readAvailable(dst: Int8Array, offset: Int, length: Int): Int {
    val remaining = remaining
    if (remaining == 0L) return -1
    val size = minOf(remaining, length.toLong()).toInt()
    readFully(dst, offset, size)
    return size
}

internal fun AbstractInput.readAvailable(dst: ArrayBuffer, offset: Int, length: Int): Int {
    val remaining = remaining
    if (remaining == 0L) return -1
    val size = minOf(remaining, length.toLong()).toInt()
    readFully(dst, offset, size)
    return size
}

internal fun AbstractInput.readAvailable(dst: ArrayBufferView, offset: Int, length: Int): Int {
    val remaining = remaining
    if (remaining == 0L) return -1
    val size = minOf(remaining, length.toLong()).toInt()
    readFully(dst, offset, size)
    return size
}
