package kotlinx.io

import kotlinx.io.buffer.copyTo
import kotlin.math.min

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
fun Output.writeUByte(value: UByte): Unit = writeByte(value.toByte())

/**
 * Write a [value] to this [Input].
 */
@ExperimentalUnsignedTypes
fun Output.writeUShort(value: UShort): Unit = writeShort(value.toShort())

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

/**
 * Write a floating-point [value] to this [Output].
 */
fun Output.writeFloat(value: Float) {
    writeInt(value.toBits())
}

/**
 * Write a double-precision [value] to this [Output].
 */
fun Output.writeDouble(value: Double) {
    writeLong(value.toBits())
}

/**
 * Copy [atMost] bytes from input to this output.
 * @return number of bytes actually written
 */
@ExperimentalIoApi
fun Output.writeInput(input: Input, atMost: Int = Binary.INFINITE): Int {
    var written = 0
    while (!input.eof() && (atMost == Binary.INFINITE || written < atMost) ) {
        input.readBufferLength { inputBuffer, inputOffset, inputSize ->
            val read = min(inputSize, atMost - written)
            var toRead = read
            while (toRead > 0) {
                writeBufferRange { outputBuffer, outputStart, outputEnd ->
                    val toWrite = min(toRead, outputEnd - outputStart)
                    inputBuffer.copyTo(outputBuffer, inputOffset, toWrite, outputStart)
                    toRead -= toWrite
                    return@writeBufferRange toWrite
                }
            }
            written += read
            return@readBufferLength inputSize
        }
    }
    flush()
    return written
}

/**
 * Write content of this binary to given output
 */
@ExperimentalIoApi
fun Binary.writeTo(output: Output) {
    read {
        output.run {
            writeInput(this@read, atMost = size)
        }
    }
}