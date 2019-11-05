package kotlinx.io

import kotlinx.io.buffer.*

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
fun Output.writeUByte(value: UByte): Unit = writeByte(value.toByte())

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
fun Output.writeUShort(value: UShort) : Unit = writeShort(value.toShort())

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
fun Output.writeUInt(value: UInt): Unit = writeInt(value.toInt())

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
fun Output.writeULong(value: ULong): Unit = writeLong(value.toLong())

/**
 * Write an [array] to this [Input].
 *
 * TODO: measure
 */
@ExperimentalUnsignedTypes
fun Output.writeArray(array: UByteArray) {
    for (byte in array) {
        writeUByte(byte)
    }
}