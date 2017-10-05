package kotlinx.io.utils

import kotlinx.io.core.*
import java.io.EOFException
import java.nio.channels.*

fun WritableByteChannel.writePacket(builder: BytePacketBuilder.() -> Unit) {
    writePacket(buildPacket(block = builder))
}

fun WritableByteChannel.writePacket(p: ByteReadPacket) {
    var b: BufferView? = null
    try {
        while (true) {
            @Suppress("INVISIBLE_MEMBER")
            b = p.steal() ?: break

            b.readDirect { bb ->
                while (bb.hasRemaining()) {
                    write(bb)
                }
            }
        }
    } finally {
        b?.release(p.pool)
        p.release()
    }
}

fun ReadableByteChannel.readPacketExact(n: Long): ByteReadPacket = readPacketImpl(n, n)
fun ReadableByteChannel.readPacketAtLeast(n: Long): ByteReadPacket = readPacketImpl(n, Long.MAX_VALUE)
fun ReadableByteChannel.readPacketAtMost(n: Long): ByteReadPacket = readPacketImpl(1L, n)

private fun ReadableByteChannel.readPacketImpl(min: Long, max: Long): ByteReadPacket {
    require(min >= 0L)
    require(min <= max)

    if (max == 0L) return ByteReadPacket.Empty

    val pool = BufferView.Pool
    val empty = BufferView.Empty
    var head: BufferView = empty
    var tail: BufferView = empty

    var read = 0L

    try {
        while (read < min || (read == min && min == 0L)) {
            val remInt = (max - read).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

            val part = tail.takeIf { it.writeRemaining.let { it > 200 || it >= remInt } } ?: pool.borrow().also {
                if (head === empty) {
                    head = it; tail = it
                }
            }
            if (tail !== part) {
                tail.next = part
                tail = part
            }

            part.writeDirect(1) { bb ->
                val l = bb.limit()
                if (bb.remaining() > remInt) {
                    bb.limit(bb.position() + remInt)
                }

                val rc = read(bb)
                if (rc == -1) throw EOFException("Premature end of stream: was read $read bytes of $min")

                bb.limit(l)
                read += rc
            }
        }
    } catch (t: Throwable) {
        @Suppress("INVISIBLE_MEMBER")
        head.releaseAll(pool)
        throw t
    }

    return ByteReadPacket(head, pool)
}

