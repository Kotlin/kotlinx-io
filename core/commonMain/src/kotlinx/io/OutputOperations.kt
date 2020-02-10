package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.math.*

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeUByte(value: UByte): Unit = writeByte(value.toByte())

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeUShort(value: UShort): Unit = writeShort(value.toShort())

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeUInt(value: UInt): Unit = writeInt(value.toInt())

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeULong(value: ULong): Unit = writeLong(value.toLong())

/**
 * Write an [array] to this [Input].
 */
public fun Output.writeByteArray(array: ByteArray, startIndex: Int = 0, endIndex: Int = array.size) {
    var offset = startIndex
    while (offset < endIndex) {
        writeBuffer { buffer, bufferStart, bufferEnd ->
            val count = min(bufferEnd - bufferStart, endIndex - offset)
            buffer.storeByteArray(bufferStart, array, offset, count)
            offset += count
            bufferStart + count
        }
    }
}

/**
 * Write an [array] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeByteArray(array: UByteArray) {
    for (byte in array) {
        writeUByte(byte)
    }
}

/**
 * Write a [value] to this [Input].
 */
public fun Output.writeShort(value: Short) {
    writePrimitive(2, { buffer, offset -> buffer.storeShortAt(offset, value) }, { value.toLong() })
}

/**
 * Write a [value] to this [Input].
 */
public fun Output.writeInt(value: Int) {
    writePrimitive(4, { buffer, offset -> buffer.storeIntAt(offset, value) }, { value.toLong() })
}

/**
 * Write a [value] to this [Input].
 */
public fun Output.writeLong(value: Long) {
    writePrimitive(8, { buffer, offset -> buffer.storeLongAt(offset, value) }) { value }
}

/**
 * Write a floating-point [value] to this [Output].
 */
public fun Output.writeFloat(value: Float) {
    writeInt(value.toBits())
}

/**
 * Write a double-precision [value] to this [Output].
 */
public fun Output.writeDouble(value: Double) {
    writeLong(value.toBits())
}
