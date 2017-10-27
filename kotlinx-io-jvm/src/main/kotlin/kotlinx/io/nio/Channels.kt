package kotlinx.io.nio

import kotlinx.io.core.*
import java.io.EOFException
import java.nio.channels.*

/**
 * Builds packet and write it to a NIO channel. May block if the channel is configured as blocking or
 * may write packet partially so this function returns remaining packet. So for blocking channel this
 * function always returns `null`.
 */
fun WritableByteChannel.writePacket(builder: BytePacketBuilder.() -> Unit): ByteReadPacket? {
    val p = buildPacket(block = builder)
    return try {
        if (writePacket(p)) null else p
    } catch (t: Throwable) {
        p.release()
        throw t
    }
}

/**
 * Writes packet to a NIO channel. May block if the channel is configured as blocking or may write packet
 * only partially if the channel is non-blocking and there is not enough buffer space.
 * @return `true` if the whole packet has been written to the channel
 */
fun WritableByteChannel.writePacket(p: ByteReadPacket): Boolean {
    try {
        while (true) {
            var rc = 0

            @Suppress("INVISIBLE_MEMBER")
            p.readDirect { node : BufferView ->
                node.readDirect {
                    rc = write(it)
                }
            }

            if (p.isEmpty) return true
            if (rc == 0) return false
        }
    } catch (t: Throwable) {
        p.release()
        throw t
    }
}

/**
 * Read a packet of exactly [n] bytes. This function is useless with non-blocking channels
 */
fun ReadableByteChannel.readPacketExact(n: Long): ByteReadPacket = readPacketImpl(n, n)

/**
 * Read a packet of at least [n] bytes or all remaining. Does fail if not enough bytes remaining.
 * . This function is useless with non-blocking channels
 */
fun ReadableByteChannel.readPacketAtLeast(n: Long): ByteReadPacket = readPacketImpl(n, Long.MAX_VALUE)

/**
 * Read a packet of at most [n] bytes. Resulting packet could be empty however this function does always reads
 * as much bytes as possible. You also can use it with non-blocking channels
 */
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

