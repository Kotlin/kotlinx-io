package kotlinx.coroutines.io

import kotlinx.io.core.*
import org.khronos.webgl.*

/**
 * Channel for asynchronous reading of sequences of bytes.
 * This is a **single-reader channel**.
 *
 * Operations on this channel cannot be invoked concurrently.
 */
actual interface ByteReadChannel {
    /**
     * Returns number of bytes that can be read without suspension. Read operations do no suspend and return
     * immediately when this number is at least the number of bytes requested for read.
     */
    actual val availableForRead: Int

    /**
     * Returns `true` if the channel is closed and no remaining bytes are available for read.
     * It implies that [availableForRead] is zero.
     */
    actual val isClosedForRead: Boolean

    actual val isClosedForWrite: Boolean

    /**
     * Byte order that is used for multi-byte read operations
     * (such as [readShort], [readInt], [readLong], [readFloat], and [readDouble]).
     */
    actual var readByteOrder: ByteOrder

    /**
     * Number of bytes read from the channel.
     * It is not guaranteed to be atomic so could be updated in the middle of long running read operation.
     */
    @Deprecated("Don't use byte count")
    actual val totalBytesRead: Long

    /**
     * Reads all available bytes to [dst] buffer and returns immediately or suspends if no bytes available
     * @return number of bytes were read or `-1` if the channel has been closed
     */
    actual suspend fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int

    actual suspend fun readAvailable(dst: IoBuffer): Int

    suspend fun readAvailable(dst: ArrayBuffer, offset: Int, length: Int): Int

    /**
     * Reads all [length] bytes to [dst] buffer or fails if channel has been closed.
     * Suspends if not enough bytes available.
     */
    actual suspend fun readFully(dst: ByteArray, offset: Int, length: Int)
    actual suspend fun readFully(dst: IoBuffer, n: Int)

    suspend fun readFully(dst: ArrayBuffer, offset: Int, length: Int)

    /**
     * Reads the specified amount of bytes and makes a byte packet from them. Fails if channel has been closed
     * and not enough bytes available. Accepts [headerSizeHint] to be provided, see [BytePacketBuilder].
     */
    actual suspend fun readPacket(size: Int, headerSizeHint: Int): ByteReadPacket

    /**
     * Reads up to [limit] bytes and makes a byte packet or until end of stream encountered.
     * Accepts [headerSizeHint] to be provided, see [BytePacketBuilder].
     */
    actual suspend fun readRemaining(limit: Long, headerSizeHint: Int): ByteReadPacket

    /**
     * Reads a long number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    actual suspend fun readLong(): Long

    /**
     * Reads an int number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    actual suspend fun readInt(): Int

    /**
     * Reads a short number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    actual suspend fun readShort(): Short

    /**
     * Reads a byte (suspending if no bytes available yet) or fails if channel has been closed
     * and not enough bytes.
     */
    actual suspend fun readByte(): Byte

    /**
     * Reads a boolean value (suspending if no bytes available yet) or fails if channel has been closed
     * and not enough bytes.
     */
    actual suspend fun readBoolean(): Boolean

    /**
     * Reads double number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    actual suspend fun readDouble(): Double

    /**
     * Reads float number (suspending if not enough bytes available) or fails if channel has been closed
     * and not enough bytes.
     */
    actual suspend fun readFloat(): Float


    /**
     * Starts non-suspendable read session. After channel preparation [consumer] lambda will be invoked immediately
     * event if there are no bytes available for read yet.
     */
    @ExperimentalIoApi
    actual fun readSession(consumer: ReadSession.() -> Unit)

    /**
     * Starts a suspendable read session. After channel preparation [consumer] lambda will be invoked immediately
     * even if there are no bytes available for read yet. [consumer] lambda could suspend as much as needed.
     */
    @ExperimentalIoApi
    actual suspend fun readSuspendableSession(consumer: suspend SuspendableReadSession.() -> Unit)

    /**
     * Reads a line of UTF-8 characters to the specified [out] buffer up to [limit] characters.
     * Supports both CR-LF and LF line endings.
     * Throws an exception if the specified [limit] has been exceeded.
     *
     * @return `true` if line has been read (possibly empty) or `false` if channel has been closed
     * and no characters were read.
     */
    actual suspend fun <A : Appendable> readUTF8LineTo(out: A, limit: Int): Boolean

    /**
     * Reads a line of UTF-8 characters up to [limit] characters.
     * Supports both CR-LF and LF line endings.
     * Throws an exception if the specified [limit] has been exceeded.
     *
     * @return a line string with no line endings or `null` of channel has been closed
     * and no characters were read.
     */
    actual suspend fun readUTF8Line(limit: Int): String?

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
    actual fun cancel(cause: Throwable?): Boolean

    /**
     * Discard up to [max] bytes
     *
     * @return number of bytes were discarded
     */
    actual suspend fun discard(max: Long): Long

    actual companion object {
        actual val Empty: ByteReadChannel by lazy {
            ByteChannelJS(IoBuffer.Empty, false).apply {
                close(null)
            }
        }
    }
}