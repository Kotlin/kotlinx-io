package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.math.*

/**
 * Writes the unsigned byte [value] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeUByte(value: UByte): Unit = writeByte(value.toByte())

/**
 * Writes the unsigned short [value] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeUShort(value: UShort): Unit = writeShort(value.toShort())

/**
 * Writes the unsigned int [value] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeUInt(value: UInt): Unit = writeInt(value.toInt())

/**
 * Writes the unsigned long [value] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeULong(value: ULong): Unit = writeLong(value.toLong())

/**
 * Writes the [array] to this [Input].
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
 * Writes the [array] to this [Input].
 */
@ExperimentalUnsignedTypes
public fun Output.writeByteArray(array: UByteArray) {
    for (byte in array) {
        writeUByte(byte)
    }
}

/**
 * Writes the short [value] to this [Input].
 */
public fun Output.writeShort(value: Short) {
    writePrimitive(2, value.toLong()) { buffer, offset -> buffer.storeShortAt(offset, value) }
}

/**
 * Writes the integer [value] to this [Input].
 */
public fun Output.writeInt(value: Int) {
    writePrimitive(4, value.toLong()) { buffer, offset -> buffer.storeIntAt(offset, value) }
}

/**
 * Writes the [value] to this [Input].
 */
public fun Output.writeLong(value: Long) {
    writePrimitive(8, value) { buffer, offset -> buffer.storeLongAt(offset, value) }
}

/**
 * Writes the floating-point [value] to this [Output].
 */
public fun Output.writeFloat(value: Float) {
    writeInt(value.toBits())
}

/**
 * Writes the double-precision [value] to this [Output].
 */
public fun Output.writeDouble(value: Double) {
    writeLong(value.toBits())
}
