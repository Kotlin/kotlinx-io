@file:Suppress("Duplicates")

package kotlinx.io.core

import kotlinx.io.bits.*

fun Input.readShort(byteOrder: ByteOrder): Short =
    readPrimitiveTemplate(byteOrder, { readShort() }, { reverseByteOrder() })

fun Input.readInt(byteOrder: ByteOrder): Int =
    readPrimitiveTemplate(byteOrder, { readInt() }, { reverseByteOrder() })

fun Input.readLong(byteOrder: ByteOrder): Long =
    readPrimitiveTemplate(byteOrder, { readLong() }, { reverseByteOrder() })

fun Input.readFloat(byteOrder: ByteOrder): Float =
    readPrimitiveTemplate(byteOrder, { readFloat() }, { reverseByteOrder() })

fun Input.readDouble(byteOrder: ByteOrder): Double =
    readPrimitiveTemplate(byteOrder, { readDouble() }, { reverseByteOrder() })

fun Input.readShortLittleEndian(): Short = readPrimitiveTemplate({ readShort() }, { reverseByteOrder() })

fun Input.readIntLittleEndian(): Int = readPrimitiveTemplate({ readInt() }, { reverseByteOrder() })

fun Input.readLongLittleEndian(): Long = readPrimitiveTemplate({ readLong() }, { reverseByteOrder() })

fun Input.readFloatLittleEndian(): Float = readPrimitiveTemplate({ readFloat() }, { reverseByteOrder() })

fun Input.readDoubleLittleEndian(): Double = readPrimitiveTemplate({ readDouble() }, { reverseByteOrder() })

fun Input.readFullyLittleEndian(dst: UShortArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyLittleEndian(dst.asShortArray(), offset, length)
}

fun Input.readFullyLittleEndian(dst: ShortArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Input.readFullyLittleEndian(dst: UIntArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyLittleEndian(dst.asIntArray(), offset, length)
}

fun Input.readFullyLittleEndian(dst: IntArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Input.readFullyLittleEndian(dst: ULongArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyLittleEndian(dst.asLongArray(), offset, length)
}

fun Input.readFullyLittleEndian(dst: LongArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Input.readFullyLittleEndian(dst: FloatArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Input.readFullyLittleEndian(dst: DoubleArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Input.readAvailableLittleEndian(dst: UShortArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailableLittleEndian(dst.asShortArray(), offset, length)
}

fun Input.readAvailableLittleEndian(dst: ShortArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    val result = readAvailable(dst, offset, length)
    if (result > 0 && byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + result - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
    return result
}

fun Input.readAvailableLittleEndian(dst: UIntArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailableLittleEndian(dst.asIntArray(), offset, length)
}

fun Input.readAvailableLittleEndian(dst: IntArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    val result = readAvailable(dst, offset, length)
    if (result > 0 && byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + result - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
    return result
}

fun Input.readAvailableLittleEndian(dst: ULongArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailableLittleEndian(dst.asLongArray(), offset, length)
}

fun Input.readAvailableLittleEndian(dst: LongArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    val result = readAvailable(dst, offset, length)
    if (result > 0 && byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + result - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
    return result
}

fun Input.readAvailableLittleEndian(dst: FloatArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    val result = readAvailable(dst, offset, length)
    if (result > 0 && byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + result - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
    return result
}

fun Input.readAvailableLittleEndian(dst: DoubleArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    val result = readAvailable(dst, offset, length)
    if (result > 0 && byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + result - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
    return result
}

private inline fun <T : Any> Input.readPrimitiveTemplate(read: () -> T, reverse: T.() -> T): T {
    return when (byteOrderDeprecated) {
        ByteOrder.LITTLE_ENDIAN -> read()
        else -> read().reverse()
    }
}

private inline fun <T : Any> Input.readPrimitiveTemplate(byteOrder: ByteOrder, read: () -> T, reverse: T.() -> T): T {
    return when {
        byteOrderDeprecated == byteOrder -> read()
        else -> read().reverse()
    }
}

@Suppress("DEPRECATION")
private inline val Input.byteOrderDeprecated get() = byteOrder
