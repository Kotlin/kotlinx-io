package kotlinx.io.streams

import kotlinx.io.core.*
import java.io.*
import java.io.EOFException

fun OutputStream.writePacket(builder: BytePacketBuilder.() -> Unit) {
    writePacket(buildPacket(block = builder))
}

fun OutputStream.writePacket(p: ByteReadPacket) {
    val s = p.remaining
    if (s == 0) return
    val buffer = ByteArray(s.coerceAtMost(4096))

    try {
        while (!p.isEmpty) {
            val size = p.readAvailable(buffer)
            write(buffer, 0, size)
        }
    } finally {
        p.release()
    }
}

fun InputStream.readPacketExact(n: Long): ByteReadPacket = readPacketImpl(n, n)
fun InputStream.readPacketAtLeast(n: Long): ByteReadPacket = readPacketImpl(n, Long.MAX_VALUE)
fun InputStream.readPacketAtMost(n: Long): ByteReadPacket = readPacketImpl(1L, n)

private fun InputStream.readPacketImpl(min: Long, max: Long): ByteReadPacket {
    require(min >= 0L)
    require(min <= max)

    val buffer = ByteArray(max.coerceAtMost(4096).toInt())
    val builder = BytePacketBuilder()

    var read = 0L

    try {
        while (read < min || (read == min && min == 0L)) {
            val remInt = minOf(max - read, Int.MAX_VALUE.toLong()).toInt()
            val rc = read(buffer, 0, minOf(remInt, buffer.size))
            if (rc == -1) throw EOFException("Premature end of stream: was read $read bytes of $min")
            read += rc
            builder.writeFully(buffer, 0, rc)
        }
    } catch (t: Throwable) {
        builder.release()
        throw t
    }

    return builder.build()
}

private val SkipBuffer = CharArray(8192)

fun ByteReadPacket.inputStream(): InputStream {
    return object : InputStream() {
        override fun read(): Int {
            if (isEmpty) return -1
            return readByte().toInt() and 0xff
        }

        override fun available() = remaining

        override fun close() {
            release()
        }
    }
}

fun ByteReadPacket.readerUTF8(): Reader {
    return object : Reader() {
        override fun close() {
            release()
        }

        override fun skip(n: Long): Long {
            var skipped = 0L
            val buffer = SkipBuffer
            val bufferSize = buffer.size

            while (skipped < n) {
                val size = minOf(bufferSize.toLong(), n - skipped).toInt()
                val rc = read(buffer, 0, size)
                if (rc == -1) break
                skipped += rc
            }

            return skipped
        }

        @Suppress("INVISIBLE_MEMBER")
        override fun read(cbuf: CharArray, off: Int, len: Int) = readCbuf(cbuf, off, len)
    }
}


fun BytePacketBuilder.outputStream(): OutputStream {
    return object : OutputStream() {
        override fun write(b: Int) {
            writeByte(b.toByte())
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            this@outputStream.writeFully(b, off, len)
        }

        override fun close() {
        }
    }
}

fun BytePacketBuilder.writerUTF8(): Writer {
    return object : Writer() {
        override fun write(cbuf: CharArray, off: Int, len: Int) {
            @Suppress("INVISIBLE_MEMBER")
            appendChars(cbuf, off, off + len)
        }

        override fun flush() {
        }

        override fun close() {
        }
    }
}
