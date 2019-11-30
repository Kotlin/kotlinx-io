package kotlinx.io.gzip

import kotlinx.io.Binary
import kotlinx.io.Input
import kotlinx.io.buffer.*
import kotlinx.io.readIt
import java.io.InputStream
import java.util.zip.InflaterInputStream

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

    override fun fill(buffer: Buffer): Int {
        repeat(buffer.size) {
            val byte: Byte = deflater.read().toByte()
            if (byte == (-1).toByte()) return it
            buffer[it] = byte
        }
        return buffer.size
    }
}

inline fun <R> Binary.readGzip(crossinline block: Input.() -> R): R = readIt { input ->
    GzipInput(input).block()
}