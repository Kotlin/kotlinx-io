package kotlinx.io.core

import kotlinx.io.internal.jvm.*
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
    val current: IoBuffer = prepareRead(1) ?: return copied

    val destinationCapacity = bb.remaining()
    val available = current.readRemaining

    return if (destinationCapacity >= available) {
        current.readFully(bb, available)
        releaseHead(current)

        readAsMuchAsPossible(bb, copied + available)
    } else {
        current.readFully(bb, destinationCapacity)
        updateHeadRemaining(current.readRemaining)
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
    val buffer = nioBuffer(size)
    val positionBefore = buffer.position()
    block(buffer)
    val delta = buffer.position() - positionBefore
    afterNioBufferUsed(delta)
}

inline fun ByteReadPacket.readDirect(size: Int, block: (ByteBuffer) -> Unit) {
    val buffer = nioBuffer(size) ?: return
    val positionBefore = buffer.position()
    try {
        block(buffer)
    } finally {
        val delta = buffer.position() - positionBefore
        afterNioBufferUsed(delta)
    }
}

inline fun AbstractInput.readDirect(size: Int, block: (ByteBuffer) -> Unit) {
    val buffer = nioBuffer(size) ?: return
    val positionBefore = buffer.position()
    try {
        block(buffer)
    } finally {
        val delta = buffer.position() - positionBefore
        afterNioBufferUsed(delta)
    }
}

@Suppress("unused")
@Deprecated("Removed", level = DeprecationLevel.HIDDEN)
inline fun ByteReadPacketBase.readDirect(size: Int, block: (ByteBuffer) -> Unit) {
    val buffer = nioBuffer(size) ?: return
    val positionBefore = buffer.position()
    try {
        block(buffer)
    } finally {
        val delta = buffer.position() - positionBefore
        afterNioBufferUsed(delta)
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

@PublishedApi
internal fun BytePacketBuilder.nioBuffer(size: Int): ByteBuffer = prepareWriteHead(size).writeBuffer

@PublishedApi
internal fun BytePacketBuilder.afterNioBufferUsed(written: Int) {
    val head = head
    if (written < 0 || written > head.writeRemaining) {
        wrongBufferPositionChangeError(written, size)
    }
    head.afterWrite()
    addSize(written)
}

@PublishedApi
internal fun ByteReadPacket.nioBuffer(size: Int): ByteBuffer? {
    return prepareRead(size)?.writeBuffer
}

@PublishedApi
internal fun AbstractInput.nioBuffer(size: Int): ByteBuffer? {
    return prepareRead(size)?.writeBuffer
}

@PublishedApi
internal fun ByteReadPacketBase.nioBuffer(size: Int): ByteBuffer? {
    return prepareRead(size)?.writeBuffer
}

@PublishedApi
internal fun ByteReadPacket.afterNioBufferUsed(read: Int) {
    (this as ByteReadPacketBase).afterNioBufferUsed(read)
}

@PublishedApi
internal fun AbstractInput.afterNioBufferUsed(read: Int) {
    (this as ByteReadPacketBase).afterNioBufferUsed(read)
}

@PublishedApi
internal fun ByteReadPacketBase.afterNioBufferUsed(read: Int) {
    val headRemaining = headRemaining
    require(read in 0..headRemaining) { "read count shouldn't be negative: $read" }
    this.headRemaining = headRemaining - read
}
