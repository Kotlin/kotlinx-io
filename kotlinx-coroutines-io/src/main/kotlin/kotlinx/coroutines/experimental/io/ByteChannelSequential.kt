package kotlinx.coroutines.experimental.io

import kotlinx.io.core.*
import kotlinx.io.core.internal.*


suspend fun ByteChannelSequentialBase.joinTo(dst: ByteChannelSequentialBase, closeOnEnd: Boolean) {
    copyTo(dst)
    if (closeOnEnd) dst.close()
}

/**
 * Reads up to [limit] bytes from receiver channel and writes them to [dst] channel.
 * Closes [dst] channel if fails to read or write with cause exception.
 * @return a number of copied bytes
 */
suspend fun ByteChannelSequentialBase.copyTo(dst: ByteChannelSequentialBase, limit: Long = Long.MAX_VALUE): Long {
    var remainingLimit = limit

    while (true) {
        if (!await(1)) break
        val transferred = transferTo(dst, limit)

        if (transferred == 0L) {
            val lastPiece = BufferView.Pool.borrow()
            lastPiece.resetForWrite(remainingLimit.toInt())
            val rc = readAvailable(lastPiece)
            if (rc == -1) {
                lastPiece.release(BufferView.Pool)
                break
            }

            dst.writeFully(lastPiece)
            lastPiece.release(BufferView.Pool)
            remainingLimit -= rc
        } else {
            remainingLimit -= transferred
        }
    }

    return limit - remainingLimit
}

/**
 * Sequential (non-concurrent) byte channel implementation
 */
abstract class ByteChannelSequentialBase(initial: BufferView, override val autoFlush: Boolean) : ByteChannel, ByteReadChannel, ByteWriteChannel, SuspendableReadSession {
    protected var closed = false
    protected val writable = BytePacketBuilder(0)
    protected var readable = ByteReadPacket(initial, BufferView.Pool)

    private val notFull = Condition { readable.remaining < 4088L }
    private var waitingForSize = 1
    private val atLeastNBytesAvailableForWrite = Condition { availableForWrite >= waitingForSize || closed }

    private var waitingForRead = 1
    private val atLeastNBytesAvailableForRead = Condition { availableForRead >= waitingForRead || closed }

    override val availableForRead: Int
        get() = readable.remaining.toInt()

    override val availableForWrite: Int
        get() = maxOf(0L, 4088 - readable.remaining).toInt()

    override var readByteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
    override var writeByteOrder: ByteOrder = ByteOrder.BIG_ENDIAN

    override val isClosedForRead: Boolean
        get() = closed && readable.isEmpty
    override val isClosedForWrite: Boolean
        get() = closed

    override val totalBytesRead: Long
        get() = 0L

    override val totalBytesWritten: Long
        get() = 0L

    final override var closedCause: Throwable? = null
        private set

    override fun flush() {
        if (writable.isNotEmpty) {
            @Suppress("DEPRECATION_ERROR")
            readable.`$unsafeAppend$`(writable)
            atLeastNBytesAvailableForRead.signal()
        }
    }

    private fun ensureNotClosed() {
        if (closed) throw ClosedWriteChannelException("Channel is already closed")
    }

    override suspend fun writeByte(b: Byte) {
        writable.writeByte(b)
        return awaitFreeSpace()
    }

    override suspend fun writeShort(s: Short) {
        writable.writeShort(s)
        return awaitFreeSpace()
    }

    override suspend fun writeInt(i: Int) {
        writable.writeInt(i)
        return awaitFreeSpace()
    }

    override suspend fun writeLong(l: Long) {
        writable.writeLong(l)
        return awaitFreeSpace()
    }

    override suspend fun writeFloat(f: Float) {
        writable.writeFloat(f)
        return awaitFreeSpace()
    }

    override suspend fun writeDouble(d: Double) {
        writable.writeDouble(d)
        return awaitFreeSpace()
    }

    override suspend fun writePacket(packet: ByteReadPacket) {
        writable.writePacket(packet)
        return awaitFreeSpace()
    }

    override suspend fun writeFully(src: BufferView) {
        writable.writeFully(src)
        return awaitFreeSpace()
    }

    override suspend fun writeFully(src: ByteArray, offset: Int, length: Int) {
        writable.writeFully(src, offset, length)
    }

    override suspend fun writeAvailable(src: BufferView): Int {
        val srcRemaining = src.readRemaining
        if (srcRemaining == 0) return 0
        val size = minOf(srcRemaining, availableForWrite)

        return if (size == 0) writeAvailableSuspend(src)
        else {
            writable.writeFully(src, size)
            size
        }
    }

    override suspend fun writeAvailable(src: ByteArray, offset: Int, length: Int): Int {
        val srcRemaining = length
        if (srcRemaining == 0) return 0
        val size = minOf(srcRemaining, availableForWrite)

        return if (size == 0) writeAvailableSuspend(src, offset, length)
        else {
            writable.writeFully(src, offset, size)
            size
        }
    }

    override suspend fun writeSuspendSession(visitor: suspend WriterSuspendSession.() -> Unit) {
        val session = object : WriterSuspendSession {
            override fun request(min: Int): BufferView? {
                @Suppress("DEPRECATION_ERROR")
                return writable.`$prepareWrite$`(min)
            }

            override fun written(n: Int) {
            }

            override fun flush() {
                this@ByteChannelSequentialBase.flush()
            }

            override suspend fun tryAwait(n: Int) {
                if (availableForWrite < n) {
                    waitingForSize = n
                    atLeastNBytesAvailableForWrite.await()
                }
            }
        }

        visitor(session)
    }

    override suspend fun readByte(): Byte {
        return if (!readable.isEmpty) {
            readable.readByte()
        } else {
            readByteSlow()
        }
    }

    private suspend fun readByteSlow(): Byte {
        do {
            waitingForRead = 1
            atLeastNBytesAvailableForRead.await()

            if (!readable.isEmpty) return readable.readByte()
        } while (true)
    }

    override suspend fun readShort(): Short {
        return if (readable.hasBytes(2)) {
            readable.readShort()
        } else {
            readShortSlow()
        }
    }

    private suspend fun readShortSlow(): Short {
        readNSlow(2) { return readable.readShort() }
    }

    override suspend fun readInt(): Int {
        return if (readable.hasBytes(4)) {
            readable.readInt()
        } else {
            readIntSlow()
        }
    }

    private suspend fun readIntSlow(): Int {
        readNSlow(4) { return readable.readInt() }
    }

    override suspend fun readLong(): Long {
        return if (readable.hasBytes(8)) {
            readable.readLong()
        } else {
            readLongSlow()
        }
    }

    private suspend fun readLongSlow(): Long {
        readNSlow(8) { return readable.readLong() }
    }

    override suspend fun readFloat(): Float {
        return if (readable.hasBytes(4)) {
            readable.readFloat()
        } else {
            readFloatSlow()
        }
    }

    private suspend fun readFloatSlow(): Float {
        readNSlow(4) { return readable.readFloat() }
    }

    override suspend fun readDouble(): Double {
        return if (readable.hasBytes(8)) {
            readable.readDouble()
        } else {
            readDoubleSlow()
        }
    }

    private suspend fun readDoubleSlow(): Double {
        readNSlow(8) { return readable.readDouble() }
    }

    override suspend fun readRemaining(limit: Long, headerSizeHint: Int): ByteReadPacket {
        val builder = BytePacketBuilder(headerSizeHint)

        builder.writePacket(readable, minOf(limit, readable.remaining))
        val remaining = limit - builder.size

        return if (remaining == 0L || (readable.isEmpty && closed)) builder.build()
        else readRemainingSuspend(builder, remaining)
    }

    private suspend fun readRemainingSuspend(builder: BytePacketBuilder, limit: Long): ByteReadPacket {
        while (builder.size < limit) {
            builder.writePacket(readable)

            if (writable.size == 0 && closed) break

            waitingForRead = 1
            atLeastNBytesAvailableForRead.await()
        }

        return builder.build()
    }

    override suspend fun readPacket(size: Int, headerSizeHint: Int): ByteReadPacket {
        val builder = BytePacketBuilder(headerSizeHint)

        var remaining = size
            val partSize = minOf(remaining.toLong(), readable.remaining).toInt()
            remaining -= partSize
            builder.writePacket(readable, partSize)

        return if (remaining > 0) readPacketSuspend(builder, remaining)
        else builder.build()
    }

    private suspend fun readPacketSuspend(builder: BytePacketBuilder, size: Int): ByteReadPacket {
        var remaining = size
        while (remaining > 0) {
            val partSize = minOf(remaining.toLong(), readable.remaining).toInt()
            remaining -= partSize
            builder.writePacket(readable, partSize)

            if (remaining > 0) {
                waitingForRead = 1
                atLeastNBytesAvailableForRead.await()
            }
        }

        return builder.build()
    }

    override suspend fun readAvailable(dst: BufferView): Int {
        return if (readable.canRead()) {
            val size = minOf(dst.writeRemaining.toLong(), readable.remaining).toInt()
            readable.readFully(dst, size)
            size
        } else if (closed) -1
        else readAvailableSuspend(dst)
    }

    private suspend fun readAvailableSuspend(dst: BufferView): Int {
        waitingForSize = 1
        atLeastNBytesAvailableForRead.await()
        return readAvailableSuspend(dst)
    }

    override suspend fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int {
        return if (readable.canRead()) {
            val size = minOf(length.toLong(), readable.remaining).toInt()
            readable.readFully(dst, size)
            size
        } else if (closed) -1
        else readAvailableSuspend(dst, offset, length)
    }

    private suspend fun readAvailableSuspend(dst: ByteArray, offset: Int, length: Int): Int {
        waitingForSize = 1
        atLeastNBytesAvailableForRead.await()
        return readAvailableSuspend(dst, offset, length)
    }

    override suspend fun readFully(dst: ByteArray, offset: Int, length: Int) {
        val rc = readAvailable(dst, offset, length)
        if (rc == length) return
        if (rc == -1) throw EOFException("Unexpected end of stream")

        return readFullySuspend(dst, offset + rc, length - rc)
    }

    private suspend fun readFullySuspend(dst: ByteArray, offset: Int, length: Int) {
        var written = 0

        while (written < length) {
            val rc = readAvailable(dst, offset + written, length - written)
            if (rc == -1) throw EOFException("Unexpected end of stream")
            written += rc
        }
    }

    override suspend fun readBoolean(): Boolean {
        if (readable.canRead()) return readable.readByte() == 1.toByte()
        else return readBooleanSlow()
    }

    private suspend fun readBooleanSlow(): Boolean {
        waitingForRead = 1
        atLeastNBytesAvailableForRead.await()
        return readBoolean()
    }

    override suspend fun await(atLeast: Int): Boolean {
        require(atLeast >= 0) { "atLeast parameter shouldn't be negative: $atLeast"}

        return if (availableForRead < atLeast) {
            awaitSuspend(atLeast)
        } else !isClosedForRead
    }

    private suspend fun awaitSuspend(atLeast: Int): Boolean {
        waitingForRead = atLeast
        atLeastNBytesAvailableForRead.await()
        return !isClosedForRead
    }

    override fun discard(n: Int): Int {
        return readable.discard(n)
    }

    override fun request(atLeast: Int): BufferView? {
        @Suppress("DEPRECATION_ERROR")
        return readable.`$prepareRead$`(atLeast)
    }

    override suspend fun discard(max: Long): Long {
        var discarded = 0L
        while (discarded <= max && !isClosedForRead) {
            discarded += readable.discard(max - discarded)
            await(1)
        }

        return discarded
    }

    override fun read(consumer: ReadSession.() -> Unit) {
        consumer(this)
    }

    override suspend fun readSuspendable(consumer: SuspendableReadSession.() -> Unit) {
        consumer(this)
    }

    override suspend fun <A : Appendable> readUTF8LineTo(out: A, limit: Int): Boolean {
        if (isClosedForRead) return false
        return decodeUTF8LineLoopSuspend(out, limit) {
            if (await(1)) readable
            else null
        }
    }

    override fun cancel(cause: Throwable?): Boolean {
        if (closedCause != null || closed) return false
        return close(cause ?: CancellationException("Channel cancelled"))
    }

    final override fun close(cause: Throwable?): Boolean {
        if (closed || closedCause != null) return false
        closedCause = cause
        closed = true
        if (cause != null) {
            readable.release()
            writable.release()
        }
        atLeastNBytesAvailableForRead.signal()
        atLeastNBytesAvailableForWrite.signal()
        return true
    }

    internal fun transferTo(dst: ByteChannelSequentialBase, limit: Long): Long {
        val size = readable.remaining
        return if (readable.remaining <= limit) {
            dst.writable.writePacket(readable)
            size
        } else {
            0
        }
    }

    private suspend inline fun readNSlow(n: Int, block: () -> Nothing): Nothing {
        do {
            waitingForRead = n
            atLeastNBytesAvailableForRead.await()

            if (readable.hasBytes(n)) block()
        } while (true)
    }

    private suspend fun writeAvailableSuspend(src: BufferView): Int {
        awaitFreeSpace()
        return writeAvailable(src)
    }

    private suspend fun writeAvailableSuspend(src: ByteArray, offset: Int, length: Int): Int {
        awaitFreeSpace()
        return writeAvailable(src, offset, length)
    }

    private suspend fun awaitFreeSpace() {
        if (closed) {
            writable.release()
            throw closedCause ?: ClosedWriteChannelException("Channel is already closed")
        }
        if (autoFlush) {
            flush()
            // TODO: avoid stealing for every byte
        }

        return notFull.await { flush() }
    }

}
