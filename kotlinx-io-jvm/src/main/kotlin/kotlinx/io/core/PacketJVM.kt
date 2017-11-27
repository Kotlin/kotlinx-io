package kotlinx.io.core

import kotlinx.io.utils.*
import java.nio.*
import java.nio.charset.*

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

fun ByteReadPacket.readText(decoder: CharsetDecoder, max: Int = Int.MAX_VALUE): String = buildString(remaining) {
    readText(decoder, this, max)
}

fun ByteReadPacket.readText(decoder: CharsetDecoder, out: Appendable, max: Int = Int.MAX_VALUE): Int {
    require(max >= 0) { "max shouldn't be negative, got $max"}

    if (out is CharBuffer && max > out.remaining()) {
        return readText(decoder, out, out.remaining())
    }

    var decoded = 0

    while (decoded < max) {
        @Suppress("INVISIBLE_MEMBER")
        readDirect { view: BufferView ->
            decoded += view.readText(decoder, out, view.next == null, max - decoded)
            if (view.readRemaining > 0 && decoded < max) {
                prepareRead(8)
            }
        }

        if (isEmpty) break
    }

    return decoded
}
