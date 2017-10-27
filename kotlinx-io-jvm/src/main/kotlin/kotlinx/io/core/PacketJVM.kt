package kotlinx.io.core

import kotlinx.io.utils.*
import java.nio.*

actual val PACKET_MAX_COPY_SIZE: Int = getIOIntProperty("max.copy.size", 500)

fun BytePacketBuilder() = BytePacketBuilder(0)
actual fun BytePacketBuilder(headerSizeHint: Int): BytePacketBuilder = BytePacketBuilder(headerSizeHint, BufferView.Pool)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias EOFException = java.io.EOFException

/**
 * Read exactly [n] (optional, read all remaining by default) bytes to a newly allocated byte buffer
 * @return a byte buffer containing [n] bytes
 */
fun ByteReadPacket.readByteBuffer(n: Int = remaining, direct: Boolean = false): ByteBuffer {
    val bb: ByteBuffer = if (direct) ByteBuffer.allocateDirect(n) else ByteBuffer.allocate(n)
    readFully(bb)
    bb.clear()
    return bb
}
