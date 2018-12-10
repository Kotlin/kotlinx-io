package kotlinx.coroutines.io

import kotlinx.io.core.*

/**
 * Channel for asynchronous reading of sequences of bytes.
 * This is a **single-reader channel**.
 *
 * Operations on this channel cannot be invoked concurrently.
 */
expect interface ByteReadChannel {
    /**
     * Returns number of bytes that can be read without suspension. Read operations do no suspend and return
     * immediately when this number is at least the number of bytes requested for read.
     */
    val availableForRead: Int

    /**
     * Returns `true` if the channel is closed and no remaining bytes are available for read.
     * It implies that [availableForRead] is zero.
     */
    val isClosedForRead: Boolean

    val isClosedForWrite: Boolean

    /**
     * Byte order that is used for multi-byte read operations
     * (such as [readShort], [readInt], [readLong], [readFloat], and [readDouble]).
     */
    var readByteOrder: ByteOrder

    /**
     * Number of bytes read from the channel.
     * It is not guaranteed to be atomic so could be updated in the middle of long running read operation.
     */
    @Deprecated("Don't use byte count")
    val totalBytesRead: Long

    /**
     * Reads all available bytes to [dst] buffer and returns immediately or suspends if no bytes available
     * @return number of bytes were read or `-1` if the channel has been closed
     */
    suspend fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int
    suspend fun readAvailable(dst: IoBuffer): Int

    /**
     * Reads all [length] bytes to [dst] buffer or fails if channel has been closed.
     * Suspends if not enough bytes available.
     */
    suspend fun readFully(dst: ByteArray, offset: Int, length: Int)
    suspend fun readFully(dst: IoBuffer, n: Int)

    /**
     * Reads the specified amount of bytes and makes a byte packet from them. Fails if channel has been closed
     * and not enough bytes available. Accepts [headerSizeHint] to be provided, see [BytePacketBuilder].
     */
    suspend fun readPacket(size: Int, headerSizeHint: Int): ByteReadPacket

    /**
     * Reads up to [limit] bytes and makes a byte packet or until end of stream encountered.
     * Accepts [headerSizeHint] to be provided, see [BytePacketBuilder].
     */
    suspend fun readRemaining(limit: Long, headerSizeHint: Int): ByteReadPacket

    /**
     * Reads a long number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    suspend fun readLong(): Long

    /**
     * Reads an int number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    suspend fun readInt(): Int

    /**
     * Reads a short number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    suspend fun readShort(): Short

    /**
     * Reads a byte (suspending if no bytes available yet) or fails if channel has been closed
     * and not enough bytes.
     */
    suspend fun readByte(): Byte

    /**
     * Reads a boolean value (suspending if no bytes available yet) or fails if channel has been closed
     * and not enough bytes.
     */
    suspend fun readBoolean(): Boolean

    /**
     * Reads double number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    suspend fun readDouble(): Double

    /**
     * Reads float number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    suspend fun readFloat(): Float


    /**
     * Starts non-suspendable read session. After channel preparation [consumer] lambda will be invoked immediately
     * event if there are no bytes available for read yet.
     */
    @ExperimentalIoApi
    fun readSession(consumer: ReadSession.() -> Unit)

    /**
     * Starts a suspendable read session. After channel preparation [consumer] lambda will be invoked immediately
     * even if there are no bytes available for read yet. [consumer] lambda could suspend as much as needed.
     */
    @ExperimentalIoApi
    suspend fun readSuspendableSession(consumer: suspend SuspendableReadSession.() -> Unit)

    /**
     * Reads a line of UTF-8 characters to the specified [out] buffer up to [limit] characters.
     * Supports both CR-LF and LF line endings. No line ending characters will be appended to [out] buffer.
     * Throws an exception if the specified [limit] has been exceeded.
     *
     * @return `true` if line has been read (possibly empty) or `false` if channel has been closed
     * and no characters were read.
     */
    suspend fun <A : Appendable> readUTF8LineTo(out: A, limit: Int): Boolean

    /**
     * Reads a line of UTF-8 characters up to [limit] characters.
     * Supports both CR-LF and LF line endings.
     * Throws an exception if the specified [limit] has been exceeded.
     *
     * @return a line string with no line endings or `null` of channel has been closed
     * and no characters were read.
     */
    suspend fun readUTF8Line(limit: Int): String?

    /**
     * Close channel with optional [cause] cancellation. Unlike [ByteWriteChannel.close] that could close channel
     * normally, cancel does always close with error so any operations on this channel will always fail
     * and all suspensions will be resumed with exception.
     *
     * Please note that if the channel has been provided by [reader] or [writer] then the corresponding owning
     * coroutine will be cancelled as well
     *
     * @see ByteWriteChannel.close
     */
    fun cancel(cause: Throwable?): Boolean

    /**
     * Discard up to [max] bytes
     *
     * @return number of bytes were discarded
     */
    suspend fun discard(max: Long): Long

    companion object {
        val Empty: ByteReadChannel
    }
}

/**
 * Reads the specified amount of bytes and makes a byte packet from them. Fails if channel has been closed
 * and not enough bytes available.
 */
suspend fun ByteReadChannel.readPacket(size: Int): ByteReadPacket = readPacket(size, 0)

/**
 * Reads up to [limit] bytes and makes a byte packet or until end of stream encountered.
 */
suspend fun ByteReadChannel.readRemaining(limit: Long): ByteReadPacket = readRemaining(limit, 0)

/**
 * Reads all remaining bytes and makes a byte packet
 */
suspend fun ByteReadChannel.readRemaining(): ByteReadPacket = readRemaining(Long.MAX_VALUE, 0)

suspend fun ByteReadChannel.readFully(dst: IoBuffer) = readFully(dst, dst.writeRemaining)

suspend fun ByteReadChannel.readUTF8LineTo(out: Appendable): Boolean {
    return readUTF8LineTo(out, Int.MAX_VALUE)
}

suspend fun ByteReadChannel.readUTF8Line(): String? {
    return readUTF8Line(Int.MAX_VALUE)
}

fun ByteReadChannel.cancel(): Boolean = cancel(null)

/**
 * Discards all bytes in the channel and suspends until end of stream.
 */
suspend fun ByteReadChannel.discard(): Long = discard(Long.MAX_VALUE)

/**
 * Discards exactly [n] bytes or fails if not enough bytes in the channel
 */
suspend inline fun ByteReadChannel.discardExact(n: Long) {
    if (discard(n) != n) throw EOFException("Unable to discard $n bytes")
}

suspend fun ByteReadChannel.readAvailable(dst: ByteArray) = readAvailable(dst, 0, dst.size)
suspend fun ByteReadChannel.readFully(dst: ByteArray) = readFully(dst, 0, dst.size)

expect suspend fun ByteReadChannel.joinTo(dst: ByteWriteChannel, closeOnEnd: Boolean)

/**
 * Reads up to [limit] bytes from receiver channel and writes them to [dst] channel.
 * Closes [dst] channel if fails to read or write with cause exception.
 * @return a number of copied bytes
 */
expect suspend fun ByteReadChannel.copyTo(dst: ByteWriteChannel, limit: Long = Long.MAX_VALUE): Long

/**
 * Reads all the bytes from receiver channel and writes them to [dst] channel and then closes it.
 * Closes [dst] channel if fails to read or write with cause exception.
 * @return a number of copied bytes
 */
suspend fun ByteReadChannel.copyAndClose(dst: ByteWriteChannel, limit: Long = Long.MAX_VALUE): Long {
    val count = copyTo(dst, limit)
    dst.close()
    return count
}
