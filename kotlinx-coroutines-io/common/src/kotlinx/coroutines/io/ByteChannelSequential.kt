package kotlinx.coroutines.io

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
    require(this !== dst)

    var remainingLimit = limit

    while (true) {
        if (!awaitInternalAtLeast1()) break
        val transferred = transferTo(dst, remainingLimit)

        val copied = if (transferred == 0L) {
            val tail = copyToTail(dst, remainingLimit)
            if (tail == 0L) break
            tail
        } else {
            if (dst.availableForWrite == 0) {
                dst.notFull.await()
            }
            transferred
        }

        remainingLimit -= copied
    }

    return limit - remainingLimit
}

private suspend fun ByteChannelSequentialBase.copyToTail(dst: ByteChannelSequentialBase, limit: Long): Long {
    val lastPiece = IoBuffer.Pool.borrow()
    try {
        lastPiece.resetForWrite(limit.coerceAtMost(lastPiece.capacity.toLong()).toInt())
        val rc = readAvailable(lastPiece)
        if (rc == -1) {
            lastPiece.release(IoBuffer.Pool)
            return 0
        }

        dst.writeFully(lastPiece)
        return rc.toLong()
    } finally {
        lastPiece.release(IoBuffer.Pool)
    }
}

/**
 * Sequential (non-concurrent) byte channel implementation
 */
@Suppress("OverridingDeprecatedMember")
@ExperimentalIoApi
abstract class ByteChannelSequentialBase(initial: IoBuffer, override val autoFlush: Boolean) : ByteChannel, ByteReadChannel, ByteWriteChannel, SuspendableReadSession {
    protected var closed = false
    protected val writable = BytePacketBuilder(0)
    protected val readable = ByteReadPacket(initial, IoBuffer.Pool)

    internal val notFull = Condition { totalPending() <= 4088L }

    private var waitingForSize = 1
    private val atLeastNBytesAvailableForWrite = Condition { availableForWrite >= waitingForSize || closed }

    private var waitingForRead = 1
    private val atLeastNBytesAvailableForRead = Condition { availableForRead >= waitingForRead || closed }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun totalPending() = readable.remaining.toInt() + writable.size

    override val availableForRead: Int
        get() = readable.remaining.toInt()

    override val availableForWrite: Int
        get() = maxOf(0, 4088 - totalPending())

    override var readByteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
        set(newOrder) {
            if (field != newOrder) {
                field = newOrder
                readable.byteOrder = newOrder
            }
        }
    override var writeByteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
        set(newOrder) {
            if (field != newOrder) {
                field = newOrder
                writable.byteOrder = newOrder
            }
        }

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

    override suspend fun writeFully(src: IoBuffer) {
        writable.writeFully(src)
        return awaitFreeSpace()
    }

    override suspend fun writeFully(src: ByteArray, offset: Int, length: Int) {
        writable.writeFully(src, offset, length)
        awaitFreeSpace()
    }

    override suspend fun writeAvailable(src: IoBuffer): Int {
        val srcRemaining = src.readRemaining
        if (srcRemaining == 0) return 0
        val size = minOf(srcRemaining, availableForWrite)

        return if (size == 0) writeAvailableSuspend(src)
        else {
            writable.writeFully(src, size)
            awaitFreeSpace()
            size
        }
    }

    override suspend fun writeAvailable(src: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val size = minOf(length, availableForWrite)

        return if (size == 0) writeAvailableSuspend(src, offset, length)
        else {
            writable.writeFully(src, offset, size)
            awaitFreeSpace()
            size
        }
    }

    @ExperimentalIoApi
    override suspend fun writeSuspendSession(visitor: suspend WriterSuspendSession.() -> Unit) {
        val session = object : WriterSuspendSession {
            override fun request(min: Int): IoBuffer? {
                if (availableForWrite == 0) return null
                return writable.prepareWriteHead(min)
            }

            override fun written(n: Int) {
                writable.afterHeadWrite()
                afterWrite()
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
        return if (readable.isNotEmpty) {
            readable.readByte().also { afterRead() }
        } else {
            readByteSlow()
        }
    }

    private fun checkClosed(n: Int) {
        if (closed) {
            throw closedCause ?: EOFException("$n bytes required but EOF reached")
        }
    }

    private suspend fun readByteSlow(): Byte {
        do {
            awaitSuspend(1)

            if (readable.isNotEmpty) return readable.readByte().also { afterRead() }
            checkClosed(1)
        } while (true)
    }

    override suspend fun readShort(): Short {
        return if (readable.hasBytes(2)) {
            readable.readShort().also { afterRead() }
        } else {
            readShortSlow()
        }
    }

    private suspend fun readShortSlow(): Short {
        readNSlow(2) { return readable.readShort().also { afterRead() } }
    }

    protected fun afterRead() {
        atLeastNBytesAvailableForWrite.signal()
        notFull.signal()
    }

    override suspend fun readInt(): Int {
        return if (readable.hasBytes(4)) {
            readable.readInt().also { afterRead() }
        } else {
            readIntSlow()
        }
    }

    private suspend fun readIntSlow(): Int {
        readNSlow(4) { return readable.readInt().also { afterRead() } }
    }

    override suspend fun readLong(): Long {
        return if (readable.hasBytes(8)) {
            readable.readLong().also { afterRead() }
        } else {
            readLongSlow()
        }
    }

    private suspend fun readLongSlow(): Long {
        readNSlow(8) { return readable.readLong().also { afterRead() } }
    }

    override suspend fun readFloat(): Float {
        return if (readable.hasBytes(4)) {
            readable.readFloat().also { afterRead() }
        } else {
            readFloatSlow()
        }
    }

    private suspend fun readFloatSlow(): Float {
        readNSlow(4) { return readable.readFloat().also { afterRead() } }
    }

    override suspend fun readDouble(): Double {
        return if (readable.hasBytes(8)) {
            readable.readDouble().also { afterRead() }
        } else {
            readDoubleSlow()
        }
    }

    private suspend fun readDoubleSlow(): Double {
        readNSlow(8) { return readable.readDouble().also { afterRead() } }
    }

    override suspend fun readRemaining(limit: Long, headerSizeHint: Int): ByteReadPacket {
        val builder = BytePacketBuilder(headerSizeHint)

        builder.writePacket(readable, minOf(limit, readable.remaining))
        val remaining = limit - builder.size

        return if (remaining == 0L || (readable.isEmpty && closed)) {
            afterRead()
            builder.build()
        }
        else readRemainingSuspend(builder, remaining)
    }

    private suspend fun readRemainingSuspend(builder: BytePacketBuilder, limit: Long): ByteReadPacket {
        while (builder.size < limit) {
            builder.writePacket(readable)
            afterRead()

            if (readable.remaining == 0L && writable.size == 0 && closed) break

            awaitSuspend(1)
        }

        return builder.build()
    }

    override suspend fun readPacket(size: Int, headerSizeHint: Int): ByteReadPacket {
        val builder = BytePacketBuilder(headerSizeHint)

        var remaining = size
        val partSize = minOf(remaining.toLong(), readable.remaining).toInt()
        remaining -= partSize
        builder.writePacket(readable, partSize)
        afterRead()

        return if (remaining > 0) readPacketSuspend(builder, remaining)
        else builder.build()
    }

    private suspend fun readPacketSuspend(builder: BytePacketBuilder, size: Int): ByteReadPacket {
        var remaining = size
        while (remaining > 0) {
            val partSize = minOf(remaining.toLong(), readable.remaining).toInt()
            remaining -= partSize
            builder.writePacket(readable, partSize)
            afterRead()

            if (remaining > 0) {
                awaitSuspend(1)
            }
        }

        return builder.build()
    }

    protected fun readAvailableClosed(): Int {
        closedCause?.let { throw it }
        -1
        return -1
    }

    override suspend fun readAvailable(dst: IoBuffer): Int {
        return when {
            closedCause != null -> throw closedCause!!
            readable.canRead() -> {
                val size = minOf(dst.writeRemaining.toLong(), readable.remaining).toInt()
                readable.readFully(dst, size)
                afterRead()
                size
            }
            closed -> readAvailableClosed()
            !dst.canWrite() -> 0
            else -> readAvailableSuspend(dst)
        }
    }

    private suspend fun readAvailableSuspend(dst: IoBuffer): Int {
        awaitSuspend(1)
        return readAvailable(dst)
    }

    override suspend fun readFully(dst: IoBuffer, n: Int) {
        require(n <= dst.writeRemaining) { "Not enough space in the destination buffer to write $n bytes" }
        require(n >= 0) { "n shouldn't be negative" }

        return when {
            closedCause != null -> throw closedCause!!
            readable.remaining >= n -> readable.readFully(dst, n).also { afterRead() }
            closed -> throw EOFException("Channel is closed and not enough bytes available: required $n but $availableForRead available")
            else -> readFullySuspend(dst, n)
        }
    }

    private suspend fun readFullySuspend(dst: IoBuffer, n: Int) {
        awaitSuspend(n)
        return readFully(dst, n)
    }

    override suspend fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int {
        return when {
            readable.canRead() -> {
                val size = minOf(length.toLong(), readable.remaining).toInt()
                readable.readFully(dst, offset, size)
                afterRead()
                size
            }
            closed -> readAvailableClosed()
            else -> readAvailableSuspend(dst, offset, length)
        }
    }

    private suspend fun readAvailableSuspend(dst: ByteArray, offset: Int, length: Int): Int {
        awaitSuspend(1)
        return readAvailable(dst, offset, length)
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
        return if (readable.canRead()) (readable.readByte() == 1.toByte()).also { afterRead() }
        else readBooleanSlow()
    }

    private suspend fun readBooleanSlow(): Boolean {
        awaitSuspend(1)
        checkClosed(1)
        return readBoolean()
    }

    private var lastReadAvailable = 0
    private var lastReadView = IoBuffer.Empty

    private fun completeReading() {
        val remaining = lastReadView.readRemaining
        val delta = lastReadAvailable - remaining
        if (lastReadView !== IoBuffer.Empty) {
            readable.updateHeadRemaining(remaining)
        }
        if (delta > 0) {
            afterRead()
        }
        lastReadAvailable = 0
        lastReadView = IoBuffer.Empty
    }

    override suspend fun await(atLeast: Int): Boolean {
        require(atLeast >= 0) { "atLeast parameter shouldn't be negative: $atLeast"}
        require(atLeast <= 4088) { "atLeast parameter shouldn't be larger than max buffer size of 4088: $atLeast" }

        completeReading()

        if (atLeast == 0) return !isClosedForRead
        if (availableForRead >= atLeast) return true

        return awaitSuspend(atLeast)
    }

    internal suspend fun awaitInternalAtLeast1(): Boolean {
        if (readable.isNotEmpty) return true
        return awaitSuspend(1)
    }

    protected suspend fun awaitSuspend(atLeast: Int): Boolean {
        require(atLeast >= 0)
        waitingForRead = atLeast
        atLeastNBytesAvailableForRead.await { afterRead() }
        closedCause?.let { throw it }
        return !isClosedForRead && availableForRead >= atLeast
    }

    override fun discard(n: Int): Int {
        closedCause?.let { throw it }
        return readable.discard(n).also { afterRead() }
    }

    override fun request(atLeast: Int): IoBuffer? {
        closedCause?.let { throw it }

        completeReading()

        val view = readable.prepareReadHead(atLeast)

        if (view == null) {
            lastReadView = IoBuffer.Empty
            lastReadAvailable = 0
        } else {
            lastReadView = view
            lastReadAvailable = view.readRemaining
        }

        view?.byteOrder = readByteOrder

        return view
    }

    override suspend fun discard(max: Long): Long {
        val discarded = readable.discard(max)

        return if (discarded == max || isClosedForRead) return discarded
        else discardSuspend(max, discarded)
    }

    private suspend fun discardSuspend(max: Long, discarded0: Long): Long {
        var discarded = discarded0

        do {
            if (!await(1)) break
            discarded += readable.discard(max - discarded)
        } while (discarded < max && !isClosedForRead)

        return discarded
    }

    @ExperimentalIoApi
    override fun readSession(consumer: ReadSession.() -> Unit) {
        try {
            consumer(this)
        } finally {
            completeReading()
        }
    }

    @ExperimentalIoApi
    override suspend fun readSuspendableSession(consumer: suspend SuspendableReadSession.() -> Unit) {
        try {
            consumer(this)
        } finally {
            completeReading()
        }
    }

    override suspend fun <A : Appendable> readUTF8LineTo(out: A, limit: Int): Boolean {
        if (isClosedForRead) return false
        @UseExperimental(DangerousInternalIoApi::class)
        return decodeUTF8LineLoopSuspend(out, limit) { size ->
            afterRead()
            if (await(size)) readable
            else null
        }
    }

    override suspend fun readUTF8Line(limit: Int): String? {
        val sb = StringBuilder()
        if (!readUTF8LineTo(sb, limit)) {
            return null
        }

        return sb.toString()
    }

    override fun cancel(cause: Throwable?): Boolean {
        if (closedCause != null || closed) return false
        return close(cause ?: kotlinx.coroutines.io.CancellationException("Channel cancelled"))
    }

    final override fun close(cause: Throwable?): Boolean {
        if (closed || closedCause != null) return false
        closedCause = cause
        closed = true
        if (cause != null) {
            readable.release()
            writable.release()
        } else {
            flush()
        }
        atLeastNBytesAvailableForRead.signal()
        atLeastNBytesAvailableForWrite.signal()
        return true
    }

    internal fun transferTo(dst: ByteChannelSequentialBase, limit: Long): Long {
        val size = readable.remaining
        return if (size <= limit) {
            dst.writable.writePacket(readable)
            dst.afterWrite()
            afterRead()
            size
        } else {
            0
        }
    }

    private suspend inline fun readNSlow(n: Int, block: () -> Nothing): Nothing {
        do {
            awaitSuspend(n)

            if (readable.hasBytes(n)) block()
            checkClosed(n)
        } while (true)
    }

    private suspend fun writeAvailableSuspend(src: IoBuffer): Int {
        awaitFreeSpace()
        return writeAvailable(src)
    }

    private suspend fun writeAvailableSuspend(src: ByteArray, offset: Int, length: Int): Int {
        awaitFreeSpace()
        return writeAvailable(src, offset, length)
    }

    protected fun afterWrite() {
        if (closed) {
            writable.release()
            throw closedCause ?: ClosedWriteChannelException("Channel is already closed")
        }
        if (autoFlush || availableForWrite == 0) {
            flush()
        }
    }

    protected suspend fun awaitFreeSpace() {
        afterWrite()
        return notFull.await { flush() }
    }
}
