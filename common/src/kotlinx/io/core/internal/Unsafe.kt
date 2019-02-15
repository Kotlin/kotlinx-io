package kotlinx.io.core.internal

import kotlinx.io.core.*

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
    if (this is ByteReadPacketBase) {
        return prepareReadHead(minSize)
    }
    if (this is ChunkBuffer) {
        return if (canRead()) this else null
    }

    return prepareReadHeadFallback(minSize)
}

private fun Input.prepareReadHeadFallback(minSize: Int): ChunkBuffer? {
    if (endOfInput) return null

    val buffer = ChunkBuffer.Pool.borrow()

    while (buffer.readRemaining < minSize) {
        val rc = peekTo(buffer)
        if (rc <= 0) {
            buffer.release(ChunkBuffer.Pool)
            return null
        }
    }

    return buffer
}

@DangerousInternalIoApi
fun Input.completeReadHead(current: ChunkBuffer) {
    if (current === this) {
        return
    }
    if (this is ByteReadPacketBase) {
        val remaining = current.readRemaining
        if (remaining == 0) {
            ensureNext(current)
        } else if (current.endGap < Buffer.ReservedSize) {
            fixGapAfterRead(current)
        } else {
            updateHeadRemaining(remaining)
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
    if (this is ByteReadPacketBase) {
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
    @Suppress("DEPRECATION")
    if (this is BytePacketBuilderBase) {
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
    @Suppress("DEPRECATION")
    if (this is BytePacketBuilderBase) {
        return afterHeadWrite()
    }

    afterWriteHeadFallback(current)
}

private fun Output.afterWriteHeadFallback(current: ChunkBuffer) {
    writeFully(current)
    current.release(ChunkBuffer.Pool)
}
