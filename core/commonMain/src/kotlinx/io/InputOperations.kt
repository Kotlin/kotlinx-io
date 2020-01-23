package kotlinx.io

import kotlinx.io.buffer.*

/**
 * Read [length] bytes from [Input] to [array] from [startIndex].
 *
 * @throws EOFException if not enough bytes available.
 */
public fun Input.readArray(array: ByteArray, startIndex: Int = 0, length: Int = array.size - startIndex) {
    var remaining = length
    var consumed = 0
    while (remaining > 0) {
        readBufferLength { buffer, offset, size ->
            val consume = minOf(size, remaining)
            buffer.loadByteArray(offset, array, startIndex + consumed, consume)
            consumed += consume
            remaining -= consume
            consume
        }
    }
}

/**
 * Read [length] bytes from [Input] to [array] from [startIndex].
 *
 * @throws EOFException if not enough bytes available.
 */
@ExperimentalUnsignedTypes
public fun Input.readArray(array: UByteArray, startIndex: Int = 0, length: Int = array.size - startIndex) {
    readArray(array.asByteArray(), startIndex, length)
}

/**
 * Reads an [UByte] from this Input.
 *
 * @throws EOFException if no more bytes can be read.
 */
@ExperimentalUnsignedTypes
public fun Input.readUByte(): UByte = readByte().toUByte()

/**
 * Reads a [ULong] from this Input.
 *
 * @throws EOFException if no more bytes can be read.
 */
@ExperimentalUnsignedTypes
public fun Input.readULong(): ULong = readLong().toULong()

/**
 * Reads an [UInt] from this Input.
 *
 * @throws EOFException if no more bytes can be read.
 */
@ExperimentalUnsignedTypes
public fun Input.readUInt(): UInt = readInt().toUInt()

/**
 * Reads an [UShort] from this Input.
 *
 * @throws EOFException if no more bytes can be read.
 */
@ExperimentalUnsignedTypes
public fun Input.readUShort(): UShort = readShort().toUShort()

/**
 * Reads a [Double] from the current [Input].
 * @throws EOFException if no more bytes can be read.
 */
public fun Input.readDouble(): Double = Double.fromBits(readLong())

/**
 * Reads a [Float] from the current [Input].
 * @throws EOFException if no more bytes can be read.
 */
public fun Input.readFloat(): Float = Float.fromBits(readInt())