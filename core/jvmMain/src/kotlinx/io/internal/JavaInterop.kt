package kotlinx.io.internal

import kotlinx.io.*
import kotlinx.io.buffer.*
import java.io.*

internal class InputStreamFromInput(private val input: Input) : InputStream() {
    override fun read(): Int {
        if (input.exhausted()) {
            return -1
        }
        return input.readByte().toInt() and 0xFF
    }

    override fun read(b: ByteArray): Int {
        if (b.isEmpty()) return 0
        val result = input.readAvailableTo(bufferOf(b))
        if (result == 0) return -1
        return result
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        val result = input.readAvailableTo(bufferOf(b), off, off + len)
        if (result == 0) return -1
        return result
    }

    override fun close() {
        input.close()
    }
}

internal class InputFromInputStream(private val inputStream: InputStream) : Input() {
    override fun closeSource() {
        inputStream.close()
    }

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        // Zero-copy attempt
        if (buffer.buffer.hasArray()) {
            val result = inputStream.read(buffer.buffer.array(), startIndex, endIndex - startIndex)
            return result.coerceAtLeast(0) // -1 when IS is closed
        }

        for (i in startIndex until endIndex) {
            val byte = inputStream.read()
            if (byte == -1) return (i - startIndex)
            buffer[i] = byte.toByte()
        }
        return endIndex - startIndex
    }
}

internal class OutputStreamFromOutput(private val output: Output) : OutputStream() {
    override fun write(b: Int) {
        output.writeByte(b.toByte())
    }

    override fun write(b: ByteArray) {
        output.writeBuffer(bufferOf(b))
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        output.writeBuffer(bufferOf(b), off, off + len)
    }

    override fun flush() {
        output.flush()
    }

    override fun close() {
        output.close()
    }
}

internal class OutputFromOutputStream(private val outputStream: OutputStream) : Output() {

    override fun closeSource() {
        outputStream.close()
    }

    override fun flush(source: Buffer, startIndex: Int, endIndex: Int): Boolean {
        if (source.buffer.hasArray()) {
            outputStream.write(source.buffer.array(), startIndex, endIndex - startIndex)
            return true
        }

        for (i in startIndex until endIndex) {
            outputStream.write(source[i].toInt())
        }

        return true
    }
}
