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
    val current: IoBuffer = prepareRead(1) ?: return copied

    val destinationCapacity = bb.remaining()
    val available = current.readRemaining

    return if (destinationCapacity >= available) {
        current.readFully(bb, available)
        @Suppress("INVISIBLE_MEMBER")
        releaseHead(current)

        readAsMuchAsPossible(bb, copied + available)
    } else {
        current.readFully(bb, destinationCapacity)
        @Suppress("DEPRECATION_ERROR")
        `$updateRemaining$`(current.readRemaining)
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
inline fun BytePacketBuilder.writeDirect(size: Int, block: (ByteBuffer) -> Unit) {
    write(size) { buffer: IoBuffer ->
        buffer.writeDirect(size, block)
    }
}

inline fun ByteReadPacket.readDirect(size: Int, block: (ByteBuffer) -> Unit) {
    read(size) { view ->
        view.readDirect {
            block(it)
        }
    }
}

/**
 * Write all [src] buffer remaining bytes and change it's position accordingly
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Deprecated("Should be resolved to member function instead", level = DeprecationLevel.HIDDEN)
fun BytePacketBuilder.writeFully(src: ByteBuffer) {
    writeFully(src)
}
