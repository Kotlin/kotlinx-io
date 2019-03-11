package kotlinx.io.core

import kotlinx.io.bits.*

fun Output.writeShortLittleEndian(value: Short) {
    writePrimitiveTemplate(value, { writeShort(it) }, { reverseByteOrder() })
}

fun Output.writeIntLittleEndian(value: Int) {
    writePrimitiveTemplate(value, { writeInt(it) }, { reverseByteOrder() })
}

fun Output.writeLongLittleEndian(value: Long) {
    writePrimitiveTemplate(value, { writeLong(it) }, { reverseByteOrder() })
}

fun Output.writeFloatLittleEndian(value: Float) {
    writePrimitiveTemplate(value, { writeFloat(it) }, { reverseByteOrder() })
}

fun Output.writeDoubleLittleEndian(value: Double) {
    writePrimitiveTemplate(value, { writeDouble(it) }, { reverseByteOrder() })
}

fun Output.writeFullyLittleEndian(source: UShortArray, offset: Int = 0, length: Int = source.size - offset) {
    writeFullyLittleEndian(source.asShortArray(), offset, length)
}

fun Output.writeFullyLittleEndian(source: ShortArray, offset: Int = 0, length: Int = source.size - offset) {
    writeArrayTemplate(
        offset,
        length,
        2,
        { writeFully(source, offset, length) },
        { writeShort(source[it].reverseByteOrder()) })
}

fun Output.writeFullyLittleEndian(source: UIntArray, offset: Int = 0, length: Int = source.size - offset) {
    writeFullyLittleEndian(source.asIntArray(), offset, length)
}

fun Output.writeFullyLittleEndian(source: IntArray, offset: Int = 0, length: Int = source.size - offset) {
    writeArrayTemplate(
        offset,
        length,
        4,
        { writeFully(source, offset, length) },
        { writeInt(source[it].reverseByteOrder()) })
}

fun Output.writeFullyLittleEndian(source: ULongArray, offset: Int = 0, length: Int = source.size - offset) {
    writeFullyLittleEndian(source.asLongArray(), offset, length)
}

fun Output.writeFullyLittleEndian(source: LongArray, offset: Int = 0, length: Int = source.size - offset) {
    writeArrayTemplate(
        offset,
        length,
        8,
        { writeFully(source, offset, length) },
        { writeLong(source[it].reverseByteOrder()) })
}

fun Output.writeFullyLittleEndian(source: FloatArray, offset: Int = 0, length: Int = source.size - offset) {
    writeArrayTemplate(
        offset,
        length,
        4,
        { writeFully(source, offset, length) },
        { writeFloat(source[it].reverseByteOrder()) })
}

fun Output.writeFullyLittleEndian(source: DoubleArray, offset: Int = 0, length: Int = source.size - offset) {
    writeArrayTemplate(
        offset,
        length,
        8,
        { writeFully(source, offset, length) },
        { writeDouble(source[it].reverseByteOrder()) })
}

private inline fun <T : Any> Output.writePrimitiveTemplate(value: T, write: (T) -> Unit, reverse: T.() -> T) {
    write(
        when (byteOrderDeprecated) {
            ByteOrder.LITTLE_ENDIAN -> value
            else -> value.reverse()
        }
    )
}


private inline fun Output.writeArrayTemplate(
    offset: Int,
    length: Int,
    componentSize: Int,
    writeFullyBE: () -> Unit,
    writeComponent: IoBuffer.(Int) -> Unit
) {
    if (byteOrderDeprecated != ByteOrder.LITTLE_ENDIAN) {
        val untilIndex = offset + length
        var start = offset
        writeWhileSize(componentSize) { buffer ->
            val size = minOf(buffer.writeRemaining / componentSize, untilIndex - start)
            val lastIndex = start + size - 1
            for (index in start..lastIndex) {
                writeComponent(buffer, index)
            }
            start += size
            when {
                start < untilIndex -> componentSize
                else -> 0
            }
        }
    } else {
        writeFullyBE()
    }
}

@Suppress("DEPRECATION")
private inline val Output.byteOrderDeprecated
    get() = byteOrder
