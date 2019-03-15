package kotlinx.io.core.internal

import kotlinx.io.core.*
import kotlin.jvm.*

/**
 * API marked with this annotation is internal and extremely fragile and not intended to be used by library users.
 * Such API could be changed without notice including rename, removal and behaviour change.
 * Also using API marked with this annotation could cause data loss or any other damage.
 */
@Experimental(level = Experimental.Level.ERROR)
annotation class DangerousInternalIoApi

@DangerousInternalIoApi
fun ByteReadPacket.`$unsafeAppend$`(builder: BytePacketBuilder) {
    val builderSize = builder.size
    val builderHead = builder.head

    if (builderSize <= PACKET_MAX_COPY_SIZE && builderHead.next == null && tryWriteAppend(builderHead)) {
        builder.afterBytesStolen()
        return
    }

    builder.stealAll()?.let { chain ->
        append(chain)
    }
}

@DangerousInternalIoApi
fun Input.prepareReadFirstHead(minSize: Int): ChunkBuffer? {
    if (this is AbstractInput) {
        return prepareReadHead(minSize)
    }
    if (this is ChunkBuffer) {
        return if (canRead()) this else null
    }

    return prepareReadHeadFallback(minSize)
}

private fun Input.prepareReadHeadFallback(minSize: Int): ChunkBuffer? {
    if (endOfInput) return null

    if (this is AbstractInput) {
        if (!prefetch(minSize)) {
            prematureEndOfStream(minSize)
        }
    }

    val buffer = ChunkBuffer.Pool.borrow()
    var copied = 0

    while (copied < minSize) {
        val rc = try {
            peekTo(buffer, copied)
        } catch (t: Throwable) {
            buffer.release(ChunkBuffer.Pool)
            throw t
        }

        if (rc <= 0) {
            buffer.release(ChunkBuffer.Pool)
            return null
        }

        copied += rc
    }

    return buffer
}

@DangerousInternalIoApi
fun Input.completeReadHead(current: ChunkBuffer) {
    if (current === this) {
        return
    }
    if (this is AbstractInput) {
        if (!current.canRead()) {
            ensureNext(current)
        } else if (current.endGap < Buffer.ReservedSize) {
            fixGapAfterRead(current)
        } else {
            headPosition = current.readPosition
        }
        return
    }

    completeReadHeadFallback(current)
}

private fun Input.completeReadHeadFallback(current: ChunkBuffer) {
    val discardAmount = current.capacity - current.writeRemaining - current.readRemaining
    discardExact(discardAmount)
    current.release(ChunkBuffer.Pool)
}

@DangerousInternalIoApi
fun Input.prepareReadNextHead(current: ChunkBuffer): ChunkBuffer? {
    if (current === this) {
        return if (canRead()) this else null
    }
    if (this is AbstractInput) {
        return ensureNextHead(current)
    }

    return prepareNextReadHeadFallback(current)
}

private fun Input.prepareNextReadHeadFallback(current: ChunkBuffer): ChunkBuffer? {
    val discardAmount = current.capacity - current.writeRemaining - current.readRemaining
    discardExact(discardAmount)
    current.resetForWrite()

    if (endOfInput || peekTo(current) <= 0) {
        current.release(ChunkBuffer.Pool)
        return null
    }

    return current
}

@DangerousInternalIoApi
fun Output.prepareWriteHead(capacity: Int, current: ChunkBuffer?): ChunkBuffer {
    if (this is AbstractOutput) {
        if (current != null) {
            afterHeadWrite()
        }
        return prepareWriteHead(capacity)
    }

    return prepareWriteHeadFallback(current)
}

private fun Output.prepareWriteHeadFallback(current: ChunkBuffer?): ChunkBuffer {
    if (current != null) {
        writeFully(current)
        current.resetForWrite()
        return current
    }

    return ChunkBuffer.Pool.borrow()
}

@DangerousInternalIoApi
fun Output.afterHeadWrite(current: ChunkBuffer) {
    if (this is AbstractOutput) {
        return afterHeadWrite()
    }

    afterWriteHeadFallback(current)
}

@JvmField
internal val EmptyByteArray = ByteArray(0)

private fun Output.afterWriteHeadFallback(current: ChunkBuffer) {
    writeFully(current)
    current.release(ChunkBuffer.Pool)
}
