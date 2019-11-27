package kotlinx.io

import kotlinx.io.buffer.copyTo
import kotlin.math.min

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
 *
 * TODO: measure
 */
@ExperimentalUnsignedTypes
public fun Output.writeArray(array: UByteArray) {
    for (byte in array) {
        writeUByte(byte)
    }
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

/**
 * Copy [atMost] bytes from input to this output.
 * @return number of bytes actually written
 */
@ExperimentalIoApi
public fun Output.writeInput(input: Input, atMost: Int = Binary.INFINITE): Int {
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
 * Write a [Binary] (including [Bytes]) to the [Output]
 */
@ExperimentalIoApi
public fun Output.writeBinary(binary: Binary){
    binary.read {
        writeInput(this, binary.size)
    }
}

/**
 * Write content of this binary to given output
 */
@ExperimentalIoApi
public fun Binary.writeTo(output: Output) {
    read {
        output.run {
            writeInput(this@read, atMost = size)
        }
    }
}