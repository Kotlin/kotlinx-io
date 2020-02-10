package kotlinx.io.gzip

import kotlinx.io.*
import kotlinx.io.buffer.*
import java.io.*
import java.util.zip.*

class GzipInput(private val original: Input) : Input() {
    private val inputStream = object : InputStream() {
        override fun read(): Int {
            if (original.eof()) return -1
            return original.readByte().toInt()
        }
    }
    private val deflater = InflaterInputStream(inputStream)

    override fun closeSource() {
    }

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        for (index in startIndex until endIndex) {
            val byte: Byte = deflater.read().toByte()
            if (byte == (-1).toByte()) return index
            buffer[index] = byte
        }
        return buffer.size
    }
}