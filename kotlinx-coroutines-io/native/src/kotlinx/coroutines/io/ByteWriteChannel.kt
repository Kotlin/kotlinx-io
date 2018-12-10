package kotlinx.coroutines.io

import kotlinx.io.core.*
import kotlinx.cinterop.*

/**
 * Channel for asynchronous writing of sequences of bytes.
 * This is a **single-writer channel**.
 *
 * Operations on this channel cannot be invoked concurrently, unless explicitly specified otherwise
 * in description. Exceptions are [close] and [flush].
 */
actual interface ByteWriteChannel {
    /**
     * Returns number of bytes that can be written without suspension. Write operations do no suspend and return
     * immediately when this number is at least the number of bytes requested for write.
     */
    actual val availableForWrite: Int

    /**
     * Returns `true` is channel has been closed and attempting to write to the channel will cause an exception.
     */
    actual val isClosedForWrite: Boolean

    /**
     * Returns `true` if channel flushes automatically all pending bytes after every write function call.
     * If `false` then flush only happens at manual [flush] invocation or when the buffer is full.
     */
    actual val autoFlush: Boolean

    /**
     * Byte order that is used for multi-byte write operations
     * (such as [writeShort], [writeInt], [writeLong], [writeFloat], and [writeDouble]).
     */
    actual var writeByteOrder: ByteOrder

    /**
     * Number of bytes written to the channel.
     * It is not guaranteed to be atomic so could be updated in the middle of write operation.
     */
    @Deprecated("Counter is no longer supported")
    actual val totalBytesWritten: Long

    /**
     * An closure cause exception or `null` if closed successfully or not yet closed
     */
    actual val closedCause: Throwable?

    /**
     * Writes as much as possible and only suspends if buffer is full
     */
    actual suspend fun writeAvailable(src: ByteArray, offset: Int, length: Int): Int

    /**
     * Writes as much as possible and only suspends if buffer is full
     */
    actual suspend fun writeAvailable(src: IoBuffer): Int

    /**
     * Writes as much as possible and only suspends if buffer is full
     */
    suspend fun writeAvailable(src: CPointer<ByteVar>, offset: Int, length: Int): Int

    /**
     * Writes as much as possible and only suspends if buffer is full
     */
    suspend fun writeAvailable(src: CPointer<ByteVar>, offset: Long, length: Long): Int

    /**
     * Writes all [src] bytes and suspends until all bytes written. Causes flush if buffer filled up or when [autoFlush]
     * Crashes if channel get closed while writing.
     */
    actual suspend fun writeFully(src: ByteArray, offset: Int, length: Int)

    /**
     * Writes all [src] bytes and suspends until all bytes written. Causes flush if buffer filled up or when [autoFlush]
     * Crashes if channel get closed while writing.
     */
    actual suspend fun writeFully(src: IoBuffer)

    /**
     * Writes all [src] bytes and suspends until all bytes written. Causes flush if buffer filled up or when [autoFlush]
     * Crashes if channel get closed while writing.
     */
    suspend fun writeFully(src: CPointer<ByteVar>, offset: Int, length: Int)

    /**
     * Writes all [src] bytes and suspends until all bytes written. Causes flush if buffer filled up or when [autoFlush]
     * Crashes if channel get closed while writing.
     */
    suspend fun writeFully(src: CPointer<ByteVar>, offset: Long, length: Long)

    @ExperimentalIoApi
    actual suspend fun writeSuspendSession(visitor: suspend WriterSuspendSession.() -> Unit)

    /**
     * Writes a [packet] fully or fails if channel get closed before the whole packet has been written
     */
    actual suspend fun writePacket(packet: ByteReadPacket)

    /**
     * Writes long number and suspends until written.
     * Crashes if channel get closed while writing.
     */
    actual suspend fun writeLong(l: Long)

    /**
     * Writes int number and suspends until written.
     * Crashes if channel get closed while writing.
     */
    actual suspend fun writeInt(i: Int)

    /**
     * Writes short number and suspends until written.
     * Crashes if channel get closed while writing.
     */
    actual suspend fun writeShort(s: Short)

    /**
     * Writes byte and suspends until written.
     * Crashes if channel get closed while writing.
     */
    actual suspend fun writeByte(b: Byte)

    /**
     * Writes double number and suspends until written.
     * Crashes if channel get closed while writing.
     */
    actual suspend fun writeDouble(d: Double)

    /**
     * Writes float number and suspends until written.
     * Crashes if channel get closed while writing.
     */
    actual suspend fun writeFloat(f: Float)

    /**
     * Closes this channel with an optional exceptional [cause].
     * It flushes all pending write bytes (via [flush]).
     * This is an idempotent operation -- repeated invocations of this function have no effect and return `false`.
     *
     * A channel that was closed without a [cause], is considered to be _closed normally_.
     * A channel that was closed with non-null [cause] is called a _failed channel_. Attempts to read or
     * write on a failed channel throw this cause exception.
     *
     * After invocation of this operation [isClosedForWrite] starts returning `true` and
     * all subsequent write operations throw [ClosedWriteChannelException] or the specified [cause].
     * However, [isClosedForRead][ByteReadChannel.isClosedForRead] on the side of [ByteReadChannel]
     * starts returning `true` only after all written bytes have been read.
     *
     * Please note that if the channel has been closed with cause and it has been provided by [reader] or [writer]
     * coroutine then the corresponding coroutine will be cancelled with [cause]. If no [cause] provided then no
     * cancellation will be propagated.
     */
    actual fun close(cause: Throwable?): Boolean

    /**
     * Flushes all pending write bytes making them available for read.
     *
     * This function is thread-safe and can be invoked in any thread at any time.
     * It does nothing when invoked on a closed channel.
     */
    actual fun flush()
}
