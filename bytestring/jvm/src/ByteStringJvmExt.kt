/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * Decodes the content of a byte string to a string using given [charset].
 *
 * @param charset the charset to decode data into a string.
 */
public fun ByteString.decodeToString(charset: Charset): String = getBackingArrayReference().toString(charset)

/**
 * Encodes a string into a byte string using [charset].
 *
 * @param charset the encoding.
 */
public fun String.encodeToByteString(charset: Charset): ByteString = ByteString.wrap(toByteArray(charset))

/**
 * Returns a new read-only heap [ByteBuffer] wrapping [this] ByteString's content.
 */
@OptIn(UnsafeByteStringApi::class)
public fun ByteString.asReadOnlyByteBuffer(): ByteBuffer {
    val data: ByteArray

    UnsafeByteStringOperations.withByteArrayUnsafe(this) {
        data = it
    }

    return ByteBuffer.wrap(data).asReadOnlyBuffer()
}

/**
 * Reads [length] bytes of data from [this] ByteBuffer starting from the current position and
 * wraps them into a new [ByteString].
 *
 * Upon successful execution, current position will advance by [length].
 *
 * @throws IndexOutOfBoundsException when [length] has negative value or its value exceeds [ByteBuffer.remaining]
 */
@OptIn(UnsafeByteStringApi::class)
public fun ByteBuffer.getByteString(length: Int = remaining()): ByteString {
    if (length < 0) {
        throw IndexOutOfBoundsException("length should be non-negative (was $length)")
    }
    if (remaining() < length) {
        throw IndexOutOfBoundsException("length ($length) exceeds remaining bytes count ({${remaining()}})")
    }
    val bytes = ByteArray(length)
    get(bytes)
    return UnsafeByteStringOperations.wrapUnsafe(bytes)
}

/**
 * Reads [length] bytes of data from [this] ByteBuffer starting from [at] index and
 * wraps them into a new [ByteString].
 *
 * This function does not update [ByteBuffer.position].
 *
 * @throws IndexOutOfBoundsException when [at] is negative, greater or equal to [ByteBuffer.limit]
 * or [at] + [length] exceeds [ByteBuffer.limit].
 */
@OptIn(UnsafeByteStringApi::class)
public fun ByteBuffer.getByteString(at: Int, length: Int): ByteString {
    checkIndexAndCapacity(at, length)
    val bytes = ByteArray(length)
    // Absolute get(byte[]) was added only in JDK 13
    for (i in 0..<length) {
        bytes[i] = get(at + i)
    }
    return UnsafeByteStringOperations.wrapUnsafe(bytes)
}

/**
 * Writes [string] into [this] ByteBuffer starting from the current position.
 *
 * Upon successfully execution [ByteBuffer.position] will advance by the length of [string].
 *
 * @throws java.nio.ReadOnlyBufferException when [this] buffer is read-only
 * @throws java.nio.BufferOverflowException when [string] can't fit into remaining space of this buffer
 */
@OptIn(UnsafeByteStringApi::class)
public fun ByteBuffer.putByteString(string: ByteString) {
    UnsafeByteStringOperations.withByteArrayUnsafe(string) {
        put(it)
    }
}

/**
 * Writes [string] into [this] ByteBuffer starting from position [at].
 *
 * This function does not update [ByteBuffer.position].
 *
 * @throws java.nio.ReadOnlyBufferException when [this] buffer is read-only
 * @throws IndexOutOfBoundsException when [at] is negative, exceeds [ByteBuffer.limit], or
 * [at] + [ByteString.size] exceeds [ByteBuffer.limit]
 */
public fun ByteBuffer.putByteString(at: Int, string: ByteString) {
    checkIndexAndCapacity(at, string.size)
    // Absolute get(byte[]) was added only in JDK 13
    for (idx in string.indices) {
        put(at + idx, string[idx])
    }
}

private fun ByteBuffer.checkIndexAndCapacity(idx: Int, length: Int) {
    if (idx < 0 || idx >= limit()) {
        throw IndexOutOfBoundsException("Index $idx is out of this ByteBuffer's bounds: [0, ${limit()})")
    }
    if (length < 0) {
        throw IndexOutOfBoundsException("length should be non-negative (was $length)")
    }
    if (idx + length > limit()) {
        throw IndexOutOfBoundsException("There's not enough space to put ByteString of length $length starting" +
                " from index $idx")
    }
}