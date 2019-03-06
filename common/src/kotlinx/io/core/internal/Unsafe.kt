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
fun Input.prepareReadFirstHead(minSize: Int): IoBuffer? {
    if (this is ByteReadPacketBase) {
        return prepareReadHead(minSize)
    }
    if (this is IoBuffer) {
        return if (canRead()) this else null
    }

    return prepareReadHeadFallback(minSize)
}

private fun Input.prepareReadHeadFallback(minSize: Int): IoBuffer? {
    if (endOfInput) return null

    val buffer = IoBuffer.Pool.borrow()

    val rc = peekTo(buffer)
    if (rc < minSize) {
        buffer.release(IoBuffer.Pool)
        return null
    }

    return buffer
}

@DangerousInternalIoApi
fun Input.completeReadHead(current: IoBuffer) {
    if (current === this) {
        return
    }
    if (this is ByteReadPacketBase) {
        val remaining = current.readRemaining
        if (remaining == 0) {
            ensureNext(current)
        } else if (current.endGap < IoBuffer.ReservedSize) {
            fixGapAfterRead(current)
        } else {
            updateHeadRemaining(remaining)
        }
        return
    }

    completeReadHeadFallback(current)
}

private fun Input.completeReadHeadFallback(current: IoBuffer) {
    val discardAmount = current.capacity - current.writeRemaining - current.readRemaining
    discardExact(discardAmount)
    current.release(IoBuffer.Pool)
}

@DangerousInternalIoApi
fun Input.prepareReadNextHead(current: IoBuffer): IoBuffer? {
    if (current === this) {
        return if (canRead()) this else null
    }
    if (this is ByteReadPacketBase) {
        return ensureNextHead(current)
    }

    return prepareNextReadHeadFallback(current)
}

private fun Input.prepareNextReadHeadFallback(current: IoBuffer): IoBuffer? {
    val discardAmount = current.capacity - current.writeRemaining - current.readRemaining
    discardExact(discardAmount)
    current.resetForWrite()

    if (endOfInput || peekTo(current) <= 0) {
        current.release(IoBuffer.Pool)
        return null
    }

    return current
}

@DangerousInternalIoApi
fun Output.prepareWriteHead(capacity: Int, current: IoBuffer?): IoBuffer {
    @Suppress("DEPRECATION_ERROR")
    if (this is BytePacketBuilderBase) {
        return prepareWriteHead(capacity)
    }

    return prepareWriteHeadFallback(current)
}

private fun Output.prepareWriteHeadFallback(current: IoBuffer?): IoBuffer {
    if (current != null) {
        writeFully(current)
        current.resetForWrite()
        return current
    }

    return IoBuffer.Pool.borrow()
}

@DangerousInternalIoApi
fun Output.afterHeadWrite(current: IoBuffer) {
    @Suppress("DEPRECATION_ERROR")
    if (this is BytePacketBuilderBase) {
        return afterHeadWrite()
    }

    afterWriteHeadFallback(current)
}

private fun Output.afterWriteHeadFallback(current: IoBuffer) {
    writeFully(current)
    current.release(IoBuffer.Pool)
}
