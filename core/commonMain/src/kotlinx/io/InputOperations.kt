package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.math.*

/**
 * Copy [Input] content to [destination].
 *
 * The [Input] wouldn't be closed at the end.
 *
 * @return transferred bytes count.
 */
public fun Input.copyTo(destination: Output): Int {
    var count = 0
    while (true) {
        val chunkSize = readAvailableTo(destination)
        if (chunkSize == 0) {
            break
        }

        count += chunkSize
    }

    return count
}

/**
 *
 * Copy [size] bytes from [Input] to [destination].
 *
 * @throws EOFException if Input can't provide enough bytes.
 * @return transferred bytes count.
 */
public fun Input.copyTo(destination: Output, size: Int): Int {
    checkSize(size)

    var remaining = size
    while (remaining > 0) {
        val chunkSize = destination.writeBuffer { buffer, startIndex, endIndex ->
            val length = min(remaining, endIndex - startIndex)
            readAvailableTo(buffer, startIndex, startIndex + length)
        }

        if (chunkSize == 0) {
            throw EOFException("Can't read more bytes from closed Input[$this]. Expected ${size - remaining} of $size")
        }

        remaining -= chunkSize
    }

    return size
}

/**
 * Read [ByteArray] of fixed [size] from [Input].
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
public fun Input.readByteArray(size: Int): ByteArray {
    checkSize(size)

    val result = ByteArray(size)
    var position = size
    while (position < size) {
        val count = readBufferRange { buffer, startOffset, endOffset ->
            val length = min(endOffset - startOffset, size - position)
            buffer.copyTo(result, startOffset, length, position)

            length
        }

        if (count == 0) {
            throw EOFException("Can't read more bytes from closed Input[$this]. Expected ${size - position} of $size")
        }

        position += count
    }

    return result
}

/**
 * Read entire [Input] to [ByteArray].
 */
public fun Input.readByteArray(): ByteArray {
    val result = ByteArrayOutput()
    while (true) {
        if (readAvailableTo(result) == 0) {
            break
        }
    }

    return result.toByteArray()
}

/**
 * Read [length] bytes from [Input] to [array] from [startIndex].
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
public fun Input.readByteArray(array: ByteArray, startIndex: Int = 0, length: Int = array.size - startIndex) {
    checkArrayStartAndLength(array, startIndex, length)

    var remaining = length
    var consumed = 0
    while (remaining > 0) {
        val count = readBufferRange { buffer, bufferStart, bufferEnd ->
            val size = minOf(bufferEnd - bufferStart, remaining)
            buffer.loadByteArray(bufferStart, array, startIndex + consumed, size)
            consumed += size
            remaining -= size
            size
        }

        if (count == 0) {
            throw EOFException("No more bytes available.")
        }
    }
}

/**
 * Read [length] bytes from [Input] to [array] from [startIndex].
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
@ExperimentalUnsignedTypes
public fun Input.readByteArray(array: UByteArray, startIndex: Int = 0, length: Int = array.size - startIndex) {
    readByteArray(array.asByteArray(), startIndex, length)
}

/**
 * Reads a [Byte] from this Input.
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
public fun Input.readByte(): Byte = readPrimitive(
    1
) { buffer, offset -> buffer.loadByteAt(offset).toLong() }.toByte()

/**
 * Reads a [Short] from this Input.
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
public fun Input.readShort(): Short = readPrimitive(
    2
) { buffer, offset -> buffer.loadShortAt(offset).toLong() }.toShort()

/**
 * Reads an [Int] from this Input.
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
public fun Input.readInt(): Int = readPrimitive(
    4
) { buffer, offset -> buffer.loadIntAt(offset).toLong() }.toInt()

/**
 * Reads a [Long] from this Input.
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
public fun Input.readLong(): Long = readPrimitive(
    8
) { buffer, offset -> buffer.loadLongAt(offset) }

/**
 * Reads an [UByte] from this Input.
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
@ExperimentalUnsignedTypes
public fun Input.readUByte(): UByte = readByte().toUByte()

/**
 * Reads a [ULong] from this Input.
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
@ExperimentalUnsignedTypes
public fun Input.readULong(): ULong = readLong().toULong()

/**
 * Reads an [UInt] from this Input.
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
@ExperimentalUnsignedTypes
public fun Input.readUInt(): UInt = readInt().toUInt()

/**
 * Reads an [UShort] from this Input.
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
@ExperimentalUnsignedTypes
public fun Input.readUShort(): UShort = readShort().toUShort()

/**
 * Reads a [Double] from the current [Input].
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
public fun Input.readDouble(): Double = Double.fromBits(readLong())

/**
 * Reads a [Float] from the current [Input].
 *
 * @throws EOFException if Input can't provide enough bytes.
 */
public fun Input.readFloat(): Float = Float.fromBits(readInt())
