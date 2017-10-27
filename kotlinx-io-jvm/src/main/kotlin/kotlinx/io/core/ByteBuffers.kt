package kotlinx.io.core

import java.nio.*

/**
 * Read at most `dst.remaining()` bytes to the specified [dst] byte buffer and change it's position accordingly
 * @return number of bytes copied
 */
fun ByteReadPacket.readAvailable(dst: ByteBuffer) = readAsMuchAsPossible(dst, 0)

/**
 * Read exactly `dst.remaining()` bytes to the specified [dst] byte buffer and change it's position accordingly
 * @return number of bytes copied
 */
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

/**
 * Write bytes directly to packet builder's segment. Generally shouldn't be used in user's code and useful for
 * efficient integration.
 *
 * Invokes [block] lambda with one byte buffer. [block] lambda should change provided's position accordingly but
 * shouldn't change any other pointers.
 *
 * @param size minimal number of bytes should be available in a buffer provided to the lambda. Should be as small as
 * possible. If [size] is too large then the function may fail because the segments size is not guaranteed to be fixed
 * and not guaranteed that is will be big enough to keep [size] bytes. However it is guaranteed that the segment size
 * is at least 8 bytes long (long integer bytes length)
 */
fun BytePacketBuilder.writeDirect(size: Int, block: (ByteBuffer) -> Unit) {
    @Suppress("INVISIBLE_MEMBER")
    write(size) { buffer: BufferView ->
        buffer.writeDirect(size, block)
    }
}

/**
 * Write all [src] buffer remaining bytes and change it's position accordingly
 */
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
