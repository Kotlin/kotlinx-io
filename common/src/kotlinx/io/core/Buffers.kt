package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import kotlin.contracts.*

/**
 * A read-write facade to actual buffer of fixed size. Multiple views could share the same actual buffer.
 * Concurrent unsafe. The only concurrent-safe operation is [release].
 * In most cases [ByteReadPacket] and [BytePacketBuilder] should be used instead.
 */
@Deprecated("Use Buffer instead.", replaceWith = ReplaceWith("Buffer", "kotlinx.io.core.Buffer"))
expect class IoBuffer : Input, Output, ChunkBuffer {

    override fun close()

    final override fun flush()

    companion object {
        /**
         * Number of bytes usually reserved in the end of chunk
         * when several instances of [ChunkBuffer] are connected into a chain (usually inside of [ByteReadPacket]
         * or [BytePacketBuilder])
         */
        @Deprecated("Use Buffer.ReservedSize instead.", ReplaceWith("Buffer.ReservedSize"))
        val ReservedSize: Int

        /**
         * The empty buffer singleton: it has zero capacity for read and write.
         */
        @Deprecated("Shouldn't be used anymore.", level = DeprecationLevel.ERROR)
        val Empty: IoBuffer

        /**
         * The default buffer pool
         */
        val Pool: ObjectPool<IoBuffer>

        /**
         * Pool that always instantiates new buffers instead of reusing it
         */
        val NoPool: ObjectPool<IoBuffer>

        /**
         * A pool that always returns [IoBuffer.Empty]
         */
        val EmptyPool: ObjectPool<IoBuffer>
    }
}

@Suppress("DEPRECATION", "DEPRECATION_ERROR")
internal object EmptyBufferPoolImpl : NoPoolImpl<IoBuffer>() {
    override fun borrow() = IoBuffer.Empty
}

internal tailrec fun ChunkBuffer?.releaseAll(pool: ObjectPool<ChunkBuffer>) {
    if (this == null) return
    release(pool)
    next.releaseAll(pool)
}

internal inline fun ChunkBuffer.forEachChunk(block: (ChunkBuffer) -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    var current = this
    do {
        block(current)
        current = current.next ?: break
    } while (true)
}

/**
 * Copy every element of the chain starting from this and setup next links.
 */
internal fun ChunkBuffer.copyAll(): ChunkBuffer {
    val copied = duplicate()
    val next = this.next ?: return copied

    return next.copyAll(copied, copied)
}

private tailrec fun ChunkBuffer.copyAll(head: ChunkBuffer, prev: ChunkBuffer): ChunkBuffer {
    val copied = duplicate()
    prev.next = copied

    val next = this.next ?: return head

    return next.copyAll(head, copied)
}

internal tailrec fun ChunkBuffer.findTail(): ChunkBuffer {
    val next = this.next ?: return this
    return next.findTail()
}

/**
 * Summarize remainings of all elements of the chain
 */
@DangerousInternalIoApi
fun ChunkBuffer.remainingAll(): Long = remainingAll(0L)

private tailrec fun ChunkBuffer.remainingAll(n: Long): Long {
    val rem = readRemaining.toLong() + n
    val next = this.next ?: return rem

    return next.remainingAll(rem)
}

internal tailrec fun ChunkBuffer.isEmpty(): Boolean {
    if (readRemaining > 0) return false
    val next = this.next ?: return true
    return next.isEmpty()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.coerceAtMostMaxInt(): Int = minOf(this, Int.MAX_VALUE.toLong()).toInt()

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.coerceAtMostMaxIntOrFail(message: String): Int {
    if (this > Int.MAX_VALUE.toLong()) throw IllegalArgumentException(message)
    return this.toInt()
}

class BufferLimitExceededException(message: String) : Exception(message)
