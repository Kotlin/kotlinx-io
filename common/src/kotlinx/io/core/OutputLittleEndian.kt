@file:Suppress("Duplicates")

package kotlinx.io.core

import kotlinx.io.bits.*

fun Output.writeShortLittleEndian(value: Short) {
    writePrimitiveTemplate(value, { writeShort(value) }, { reverseByteOrder() })
}

fun Output.writeIntLittleEndian(value: Int) {
    writePrimitiveTemplate(value, { writeInt(value) }, { reverseByteOrder() })
}

fun Output.writeLongLittleEndian(value: Long) {
    writePrimitiveTemplate(value, { writeLong(value) }, { reverseByteOrder() })
}

fun Output.writeFloatLittleEndian(value: Float) {
    writePrimitiveTemplate(value, { writeFloat(value) }, { reverseByteOrder() })
}

fun Output.writeDoubleLittleEndian(value: Double) {
    writePrimitiveTemplate(value, { writeDouble(value) }, { reverseByteOrder() })
}

fun Output.writeFullyLittleEndian(dst: UShortArray, offset: Int = 0, length: Int = dst.size - offset) {
    writeFully(dst.asShortArray(), offset, length)
}

fun Output.writeFullyLittleEndian(dst: ShortArray, offset: Int = 0, length: Int = dst.size - offset) {
    writeFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Output.writeFullyLittleEndian(dst: UIntArray, offset: Int = 0, length: Int = dst.size - offset) {
    writeFully(dst.asIntArray(), offset, length)
}

fun Output.writeFullyLittleEndian(dst: IntArray, offset: Int = 0, length: Int = dst.size - offset) {
    writeFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Output.writeFullyLittleEndian(dst: ULongArray, offset: Int = 0, length: Int = dst.size - offset) {
    writeFully(dst.asLongArray(), offset, length)
}

fun Output.writeFullyLittleEndian(dst: LongArray, offset: Int = 0, length: Int = dst.size - offset) {
    writeFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Output.writeFullyLittleEndian(dst: FloatArray, offset: Int = 0, length: Int = dst.size - offset) {
    writeFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

fun Output.writeFullyLittleEndian(dst: DoubleArray, offset: Int = 0, length: Int = dst.size - offset) {
    writeFully(dst, offset, length)
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val lastIndex = offset + length - 1
        for (index in offset..lastIndex) {
            dst[index] = dst[index].reverseByteOrder()
        }
    }
}

private fun <T : Any> Output.writePrimitiveTemplate(value: T, write: (T) -> Unit, reverse: T.() -> T) {
    write(
        when (byteOrderDeprecated) {
            ByteOrder.LITTLE_ENDIAN -> value
            else -> value.reverse()
        }
    )
}

@Suppress("DEPRECATION")
private inline val Output.byteOrderDeprecated
    get() = byteOrder
