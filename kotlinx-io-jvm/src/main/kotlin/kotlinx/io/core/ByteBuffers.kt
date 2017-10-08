package kotlinx.io.core

import java.nio.*

fun ByteReadPacket.readAvailable(dst: ByteBuffer) = readAsMuchAsPossible(dst, 0)
fun ByteReadPacket.readFully(dst: ByteBuffer): Int {
    val rc = readAsMuchAsPossible(dst, 0)
    if (dst.hasRemaining()) throw EOFException("Not enough data in packet to fill buffer: ${dst.remaining()} more bytes required")
    return rc
}

private tailrec fun ByteReadPacket.readAsMuchAsPossible(bb: ByteBuffer, copied: Int): Int {
    if (!bb.hasRemaining()) return copied
    @Suppress("INVISIBLE_MEMBER")
    val current: BufferView = prepareRead(1) ?: return copied

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
