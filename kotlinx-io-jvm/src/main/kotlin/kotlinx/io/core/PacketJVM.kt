package kotlinx.io.core

import java.io.*
import java.nio.*

actual enum class ByteOrder(val nioOrder: java.nio.ByteOrder) {
    BIG_ENDIAN(java.nio.ByteOrder.BIG_ENDIAN),
    LITTLE_ENDIAN(java.nio.ByteOrder.LITTLE_ENDIAN);

    actual companion object {
        private val native: ByteOrder = orderOf(java.nio.ByteOrder.nativeOrder())
        fun of(nioOrder: java.nio.ByteOrder): ByteOrder = orderOf(nioOrder)

        actual fun nativeOrder(): ByteOrder = native
    }
}

private fun orderOf(nioOrder: java.nio.ByteOrder): ByteOrder = if (nioOrder === java.nio.ByteOrder.BIG_ENDIAN) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN

actual val PACKET_MAX_COPY_SIZE: Int = 500

fun BytePacketBuilder() = BytePacketBuilder(0)
actual fun BytePacketBuilder(headerSizeHint: Int): BytePacketBuilder = BytePacketBuilder(headerSizeHint, BufferView.Pool)

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

        override fun read(cbuf: CharArray, off: Int, len: Int) = readCbuf(cbuf, off, len)
    }
}

fun ByteReadPacket.readAvailable(dst: ByteBuffer) = readAsMuchAsPossible(dst, 0)
fun ByteReadPacket.readFully(dst: ByteBuffer): Int {
    val rc = readAsMuchAsPossible(dst, 0)
    if (dst.hasRemaining()) throw EOFException("Not enough data in packet to fill buffer: ${dst.remaining()} more bytes required")
    return rc
}

private tailrec fun ByteReadPacket.readAsMuchAsPossible(bb: ByteBuffer, copied: Int): Int {
    if (!bb.hasRemaining()) return copied
    @Suppress("USELESS_CAST")
    val current = prepareRead(1) as? BufferView ?: return copied

    val destinationCapacity = bb.remaining()
    val available = current.readRemaining

    return if (destinationCapacity >= available) {
        current.read(bb, available)
        @Suppress("INVISIBLE_MEMBER")
        releaseHead(current)

        readAsMuchAsPossible(bb, copied + available)
    } else {
        current.read(bb, destinationCapacity)
        copied + destinationCapacity
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

fun BytePacketBuilder.writeDirect(size: Int, block: (ByteBuffer) -> Unit) {
    @Suppress("INVISIBLE_MEMBER")
    write(size) { buffer: BufferView ->
        buffer.writeDirect(size, block)
    }
}

fun BytePacketBuilder.writeFully(src: ByteBuffer) {
    while (src.hasRemaining()) {
        @Suppress("INVISIBLE_MEMBER")
        write(1) { buffer: BufferView ->
            val srcSize = src.remaining()
            val capacity = buffer.writeRemaining

            if (capacity >= srcSize) {
                buffer.write(src)
                srcSize
            } else {
                val lim = src.limit()
                src.limit(src.position() + capacity)
                buffer.write(src)
                src.limit(lim)
                capacity
            }
        }
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias EOFException = java.io.EOFException
