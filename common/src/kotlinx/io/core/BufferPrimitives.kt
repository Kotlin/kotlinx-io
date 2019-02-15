package kotlinx.io.core

import kotlinx.io.bits.*
import kotlin.contracts.*
import kotlinx.io.core.internal.require

/**
 * Read an unsigned byte or fail if no bytes available for reading.
 */
fun Buffer.readUByte(): UByte = readByte().toUByte()

/**
 * Write an unsigned byte or fail if not enough space available for writing.
 */
fun Buffer.writeUByte(value: UByte) {
    writeByte(value.toByte())
}

/**
 * Read a short integer or fail if no bytes available for reading.
 * The numeric value is decoded in the network order (Big Endian).
 */
fun Buffer.readShort(): Short = readExact(2, "short integer") { memory, offset ->
    memory.loadShortAt(offset)
}

/**
 * Read an unsigned short integer or fail if not enough bytes available for reading.
 * The numeric value is decoded in the network order (Big Endian).
 */
fun Buffer.readUShort(): UShort = readExact(2, "short unsigned integer") { memory, offset ->
    memory.loadUShortAt(offset)
}

/**
 * Read an integer or fail if not enough bytes available for reading.
 * The numeric value is decoded in the network order (Big Endian).
 */
fun Buffer.readInt(): Int = readExact(4, "regular integer") { memory, offset ->
    memory.loadIntAt(offset)
}

/**
 * Read an unsigned integer or fail if not enough bytes available for reading.
 * The numeric value is decoded in the network order (Big Endian).
 */
fun Buffer.readUInt(): UInt = readExact(4, "regular unsigned integer") { memory, offset ->
    memory.loadUIntAt(offset)
}

/**
 * Read a long integer or fail if not enough bytes available for reading.
 * The numeric value is decoded in the network order (Big Endian).
 */
fun Buffer.readLong(): Long = readExact(8, "long integer") { memory, offset ->
    memory.loadLongAt(offset)
}

/**
 * Read an unsigned long integer or fail if not enough bytes available for reading.
 * The numeric value is decoded in the network order (Big Endian).
 */
fun Buffer.readULong(): ULong = readExact(8, "long unsigned integer") { memory, offset ->
    memory.loadULongAt(offset)
}

/**
 * Read a floating point number or fail if not enough bytes available for reading.
 * The numeric value is decoded in the network order (Big Endian).
 */
fun Buffer.readFloat(): Float = readExact(4, "floating point number") { memory, offset ->
    memory.loadFloatAt(offset)
}

/**
 * Read a floating point number or fail if not enough bytes available for reading.
 * The numeric value is decoded in the network order (Big Endian).
 */
fun Buffer.readDouble(): Double = readExact(8, "long floating point number") { memory, offset ->
    memory.loadDoubleAt(offset)
}

/**
 * Write a short integer or fail if not enough space available for writing.
 * The numeric value is encoded in the network order (Big Endian).
 */
fun Buffer.writeShort(value: Short): Unit = writeExact(2, "short integer") { memory, offset ->
    memory.storeShortAt(offset, value)
}

/**
 * Write an unsigned short integer or fail if not enough space available for writing.
 * The numeric value is encoded in the network order (Big Endian).
 */
fun Buffer.writeUShort(value: UShort): Unit = writeExact(2, "short unsigned integer") { memory, offset ->
    memory.storeUShortAt(offset, value)
}

/**
 * Write an integer or fail if not enough space available for writing.
 * The numeric value is encoded in the network order (Big Endian).
 */
fun Buffer.writeInt(value: Int): Unit = writeExact(4, "regular integer") { memory, offset ->
    memory.storeIntAt(offset, value)
}

/**
 * Write an unsigned integer or fail if not enough space available for writing.
 * The numeric value is encoded in the network order (Big Endian).
 */
fun Buffer.writeUInt(value: UInt): Unit = writeExact(4, "regular unsigned integer") { memory, offset ->
    memory.storeUIntAt(offset, value)
}

/**
 * Write a long integer or fail if not enough space available for writing.
 * The numeric value is encoded in the network order (Big Endian).
 */
fun Buffer.writeLong(value: Long): Unit = writeExact(8, "long integer") { memory, offset ->
    memory.storeLongAt(offset, value)
}

/**
 * Write an unsigned long integer or fail if not enough space available for writing.
 * The numeric value is encoded in the network order (Big Endian).
 */
fun Buffer.writeULong(value: ULong): Unit = writeExact(8, "long unsigned integer") { memory, offset ->
    memory.storeULongAt(offset, value)
}

/**
 * Write a floating point number or fail if not enough space available for writing.
 * The numeric value is encoded in the network order (Big Endian).
 */
fun Buffer.writeFloat(value: Float): Unit = writeExact(4, "floating point number") { memory, offset ->
    memory.storeFloatAt(offset, value)
}

/**
 * Write a floating point number or fail if not enough space available for writing.
 * The numeric value is encoded in the network order (Big Endian).
 */
fun Buffer.writeDouble(value: Double): Unit = writeExact(8, "long floating point number") { memory, offset ->
    memory.storeDoubleAt(offset, value)
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 */
fun Buffer.readFully(destination: ByteArray, offset: Int = 0, length: Int = destination.size - offset) {
    readExact(length, "byte array") { memory, srcOffset ->
        memory.loadByteArray(srcOffset, destination, offset, length)
    }
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 */
fun Buffer.readFully(destination: UByteArray, offset: Int = 0, length: Int = destination.size - offset) {
    readFully(destination.asByteArray(), offset, length)
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] bytes.
 * If less than [length] bytes available then less bytes will be copied and the corresponding number
 * will be returned as result.
 * @return number of bytes copied to the [destination] or `-1` if the buffer is empty
 */
fun Buffer.readAvailable(destination: ByteArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= destination.size) {
        "offset + length should be less than the destination size: $offset" +
            " + $length > ${destination.size}"
    }

    if (!canRead()) return -1
    val toBeRead = minOf(length, readRemaining)
    readFully(destination, offset, toBeRead)
    return toBeRead
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] bytes.
 * If less than [length] bytes available then less bytes will be copied and the corresponding number
 * will be returned as result.
 * @return number of bytes copied to the [destination] or `-1` if the buffer is empty
 */
fun Buffer.readAvailable(destination: UByteArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    return readAvailable(destination.asByteArray(), offset, length)
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified bytes [length].
 */
fun Buffer.writeFully(source: ByteArray, offset: Int = 0, length: Int = source.size - offset) {
    writeExact(length, "byte array") { memory, dstOffset ->
        memory.storeByteArray(dstOffset, source, offset, length)
    }
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified bytes [length].
 */
fun Buffer.writeFully(source: UByteArray, offset: Int = 0, length: Int = source.size - offset) {
    writeFully(source.asByteArray(), offset, length)
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.readFully(destination: ShortArray, offset: Int = 0, length: Int = destination.size - offset) {
    readExact(length * 2, "short integers array") { memory, srcOffset ->
        memory.loadShortArray(srcOffset, destination, offset, length)
    }
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.readFully(destination: UShortArray, offset: Int = 0, length: Int = destination.size - offset) {
    readFully(destination.asShortArray(), offset, length)
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] elements.
 * If less than [length] elements available then less elements will be copied and the corresponding number
 * will be returned as result (possibly zero).
 *
 * @return number of elements copied to the [destination] or `-1` if the buffer is empty,
 *  `0` - not enough bytes for at least an element
 */
fun Buffer.readAvailable(destination: ShortArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= destination.size) {
        "offset + length should be less than the destination size: $offset" +
            " + $length > ${destination.size}"
    }

    if (!canRead()) return -1
    val toBeRead = minOf(length / 2, readRemaining)
    readFully(destination, offset, toBeRead)
    return toBeRead
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] elements.
 * If less than [length] elements available then less elements will be copied and the corresponding number
 * will be returned as result (possibly zero).
 *
 * @return number of elements copied to the [destination] or `-1` if the buffer is empty,
 *  `0` - not enough bytes for at least an element
 */
fun Buffer.readAvailable(destination: UShortArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    return readAvailable(destination.asShortArray(), offset, length)
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified items [length].
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.writeFully(source: ShortArray, offset: Int = 0, length: Int = source.size - offset) {
    writeExact(length * 2, "short integers array") { memory, dstOffset ->
        memory.storeShortArray(dstOffset, source, offset, length)
    }
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified items [length].
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.writeFully(source: UShortArray, offset: Int = 0, length: Int = source.size - offset) {
    writeFully(source.asShortArray(), offset, length)
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.readFully(destination: IntArray, offset: Int = 0, length: Int = destination.size - offset) {
    readExact(length * 4, "integers array") { memory, srcOffset ->
        memory.loadIntArray(srcOffset, destination, offset, length)
    }
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.readFully(destination: UIntArray, offset: Int = 0, length: Int = destination.size - offset) {
    readFully(destination.asIntArray(), offset, length)
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] elements.
 * If less than [length] elements available then less elements will be copied and the corresponding number
 * will be returned as result (possibly zero).
 *
 * @return number of elements copied to the [destination] or `-1` if the buffer is empty,
 *  `0` - not enough bytes for at least an element
 */
fun Buffer.readAvailable(destination: IntArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= destination.size) {
        "offset + length should be less than the destination size: $offset" +
            " + $length > ${destination.size}"
    }

    if (!canRead()) return -1
    val toBeRead = minOf(length / 4, readRemaining)
    readFully(destination, offset, toBeRead)
    return toBeRead
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] elements.
 * If less than [length] elements available then less elements will be copied and the corresponding number
 * will be returned as result (possibly zero).
 *
 * @return number of elements copied to the [destination] or `-1` if the buffer is empty,
 *  `0` - not enough bytes for at least an element
 */
fun Buffer.readAvailable(destination: UIntArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    return readAvailable(destination.asIntArray(), offset, length)
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified items [length].
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.writeFully(source: IntArray, offset: Int = 0, length: Int = source.size - offset) {
    writeExact(length * 4, "integers array") { memory, dstOffset ->
        memory.storeIntArray(dstOffset, source, offset, length)
    }
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified items [length].
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.writeFully(source: UIntArray, offset: Int = 0, length: Int = source.size - offset) {
    writeFully(source.asIntArray(), offset, length)
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.readFully(destination: LongArray, offset: Int = 0, length: Int = destination.size - offset) {
    readExact(length * 8, "long integers array") { memory, srcOffset ->
        memory.loadLongArray(srcOffset, destination, offset, length)
    }
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.readFully(destination: ULongArray, offset: Int = 0, length: Int = destination.size - offset) {
    readFully(destination.asLongArray(), offset, length)
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] elements.
 * If less than [length] elements available then less elements will be copied and the corresponding number
 * will be returned as result (possibly zero).
 *
 * @return number of elements copied to the [destination] or `-1` if the buffer is empty,
 *  `0` - not enough bytes for at least an element
 */
fun Buffer.readAvailable(destination: LongArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= destination.size) {
        "offset + length should be less than the destination size: $offset" +
            " + $length > ${destination.size}"
    }

    if (!canRead()) return -1
    val toBeRead = minOf(length / 8, readRemaining)
    readFully(destination, offset, toBeRead)
    return toBeRead
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] elements.
 * If less than [length] elements available then less elements will be copied and the corresponding number
 * will be returned as result (possibly zero).
 *
 * @return number of elements copied to the [destination] or `-1` if the buffer is empty,
 *  `0` - not enough bytes for at least an element
 */
fun Buffer.readAvailable(destination: ULongArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    return readAvailable(destination.asLongArray(), offset, length)
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified items [length].
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.writeFully(source: LongArray, offset: Int = 0, length: Int = source.size - offset) {
    writeExact(length * 8, "long integers array") { memory, dstOffset ->
        memory.storeLongArray(dstOffset, source, offset, length)
    }
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified items [length].
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.writeFully(source: ULongArray, offset: Int = 0, length: Int = source.size - offset) {
    writeFully(source.asLongArray(), offset, length)
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.readFully(destination: FloatArray, offset: Int = 0, length: Int = destination.size - offset) {
    readExact(length * 4, "floating point numbers array") { memory, srcOffset ->
        memory.loadFloatArray(srcOffset, destination, offset, length)
    }
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] elements.
 * If less than [length] elements available then less elements will be copied and the corresponding number
 * will be returned as result (possibly zero).
 *
 * @return number of elements copied to the [destination] or `-1` if the buffer is empty,
 *  `0` - not enough bytes for at least an element
 */
fun Buffer.readAvailable(destination: FloatArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= destination.size) {
        "offset + length should be less than the destination size: $offset" +
            " + $length > ${destination.size}"
    }

    if (!canRead()) return -1
    val toBeRead = minOf(length / 4, readRemaining)
    readFully(destination, offset, toBeRead)
    return toBeRead
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified items [length].
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.writeFully(source: FloatArray, offset: Int = 0, length: Int = source.size - offset) {
    writeExact(length * 4, "floating point numbers array") { memory, dstOffset ->
        memory.storeFloatArray(dstOffset, source, offset, length)
    }
}

/**
 * Read from this buffer to the [destination] array to [offset] and [length] bytes.
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.readFully(destination: DoubleArray, offset: Int = 0, length: Int = destination.size - offset) {
    readExact(length * 8, "floating point numbers array") { memory, srcOffset ->
        memory.loadDoubleArray(srcOffset, destination, offset, length)
    }
}

/**
 * Read available for read bytes to the [destination] array range starting at array [offset] and [length] elements.
 * If less than [length] elements available then less elements will be copied and the corresponding number
 * will be returned as result (possibly zero).
 *
 * @return number of elements copied to the [destination] or `-1` if the buffer is empty,
 *  `0` - not enough bytes for at least an element
 */
fun Buffer.readAvailable(destination: DoubleArray, offset: Int = 0, length: Int = destination.size - offset): Int {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(offset + length <= destination.size) {
        "offset + length should be less than the destination size: $offset" +
            " + $length > ${destination.size}"
    }

    if (!canRead()) return -1
    val toBeRead = minOf(length / 8, readRemaining)
    readFully(destination, offset, toBeRead)
    return toBeRead
}

/**
 * Write the whole [source] array range staring at [offset] and having the specified items [length].
 * Numeric values are interpreted in the network byte order (Big Endian).
 */
fun Buffer.writeFully(source: DoubleArray, offset: Int = 0, length: Int = source.size - offset) {
    writeExact(length * 8, "floating point numbers array") { memory, dstOffset ->
        memory.storeDoubleArray(dstOffset, source, offset, length)
    }
}

/**
 * Read at most [length] bytes from this buffer to the [dst] buffer.
 * @return number of bytes copied
 */
fun Buffer.readFully(dst: Buffer, length: Int = dst.writeRemaining): Int {
    require(length >= 0)
    require(length <= dst.writeRemaining)

    readExact(length, "buffer content") { memory, offset ->
        memory.copyTo(dst.memory, offset, length, dst.writePosition)
        dst.commitWritten(length)
    }

    return length
}

/**
 * Read at most [length] available bytes to the [dst] buffer or `-1` if no bytes available for read.
 * @return number of bytes copied or `-1` if empty
 */
fun Buffer.readAvailable(dst: Buffer, length: Int = dst.writeRemaining): Int {
    if (!canRead()) return -1

    val readSize = minOf(dst.writeRemaining, readRemaining, length)

    readExact(readSize, "buffer content") { memory, offset ->
        memory.copyTo(dst.memory, offset, readSize, dst.writePosition)
        dst.commitWritten(readSize)
    }

    return readSize
}

/**
 * Write all readable bytes from [src] to this buffer. Fails if not enough space available to write all bytes.
 */
fun Buffer.writeFully(src: Buffer) {
    val length = src.readRemaining

    writeExact(length, "buffer readable content") { memory, offset ->
        src.memory.copyTo(memory, src.readPosition, length, offset)
        src.discard(length)
    }
}

/**
 * Write at most [length] readable bytes from [src] to this buffer.
 * Fails if not enough space available to write all bytes.
 */
fun Buffer.writeFully(src: Buffer, length: Int) {
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(length <= src.readRemaining) {
        "length shouldn't be greater than the source read remaining: $length > ${src.readRemaining}"
    }
    require(length <= writeRemaining) {
        "length shouldn't be greater than the destination write remaining space: $length > $writeRemaining"
    }

    writeExact(length, "buffer readable content") { memory, offset ->
        src.memory.copyTo(memory, src.readPosition, length, offset)
        src.discard(length)
    }
}

@PublishedApi
internal inline fun <R> Buffer.readExact(size: Int, name: String, block: (memory: Memory, offset: Int) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    var value: R
    read { memory, start, endExclusive ->
        kotlinx.io.core.internal.require(endExclusive - start >= size) {
            throw EOFException("Not enough bytes to read a $name of size $size.")
        }
        value = block(memory, start)
        size
    }

    return value
}

@PublishedApi
internal inline fun Buffer.writeExact(size: Int, name: String, block: (memory: Memory, offset: Int) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    write { memory, start, endExclusive ->
        val writeRemaining = endExclusive - start
        if (writeRemaining < size) {
            throw InsufficientSpaceException(name, size, writeRemaining)
        }
        block(memory, start)
        size
    }
}
