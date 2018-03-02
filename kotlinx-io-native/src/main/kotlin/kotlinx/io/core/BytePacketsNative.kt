package kotlinx.io.core

import kotlinx.cinterop.*

/**
 * Read at most [limit] bytes to the specified [dst] address
 * @return number of bytes copied
 */
fun ByteReadPacket.readAvailable(dst: CPointer<ByteVar>, limit: Int) = readAsMuchAsPossible(dst, limit, 0)

/**
 * Read exactly [size] bytes to the specified [dst] address
 * @return number of bytes copied
 */
fun ByteReadPacket.readFully(dst: CPointer<ByteVar>, size: Int): Int {
    val rc = readAsMuchAsPossible(dst, size, 0)
    if (rc != size) throw EOFException("Not enough data in packet to fill buffer: ${size - rc} more bytes required")
    return rc
}

private tailrec fun ByteReadPacket.readAsMuchAsPossible(buffer: CPointer<ByteVar>, destinationCapacity: Int, copied: Int): Int {
    if (destinationCapacity == 0) return copied
    @Suppress("INVISIBLE_MEMBER")
    val current: BufferView = prepareRead(1) ?: return copied

    val available = current.readRemaining

    return if (destinationCapacity >= available) {
        current.read(buffer, 0, available)
        @Suppress("INVISIBLE_MEMBER")
        releaseHead(current)

        readAsMuchAsPossible((buffer + available)!!, destinationCapacity - available, copied + available)
    } else {
        current.read(buffer, 0, destinationCapacity)
        copied + destinationCapacity
    }
}

/**
 * Write all [src] buffer remaining bytes and change it's position accordingly
 */
fun BytePacketBuilder.writeFully(src: CPointer<ByteVar>, size: Int) {
    var remaining = size
    var offset = 0

    while (remaining > 0) {
        @Suppress("INVISIBLE_MEMBER")
        write(1) { buffer: BufferView ->
            val srcSize = remaining
            val capacity = buffer.writeRemaining

            val partSize = minOf(srcSize, capacity)
            buffer.write(src, offset, partSize)
            offset += partSize
            remaining -= partSize

            partSize
        }
    }
}
