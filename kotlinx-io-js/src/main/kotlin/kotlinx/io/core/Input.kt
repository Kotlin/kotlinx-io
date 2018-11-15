package kotlinx.io.core

import kotlinx.io.core.internal.*
import org.khronos.webgl.*

/**
 * Shouldn't be implemented directly. Inherit [AbstractInput] instead.
 */
actual interface Input : Closeable {
    actual var byteOrder: ByteOrder
    actual val endOfInput: Boolean

    actual fun readByte(): Byte
    actual fun readShort(): Short
    actual fun readInt(): Int
    actual fun readLong(): Long
    actual fun readFloat(): Float
    actual fun readDouble(): Double

    actual fun readFully(dst: ByteArray, offset: Int, length: Int)
    actual fun readFully(dst: ShortArray, offset: Int, length: Int)
    actual fun readFully(dst: IntArray, offset: Int, length: Int)
    actual fun readFully(dst: LongArray, offset: Int, length: Int)
    actual fun readFully(dst: FloatArray, offset: Int, length: Int)
    actual fun readFully(dst: DoubleArray, offset: Int, length: Int)
    actual fun readFully(dst: IoBuffer, length: Int)

    fun readFully(dst: Int8Array, offset: Int, length: Int)
    fun readFully(dst: ArrayBuffer, offset: Int, length: Int)
    fun readFully(dst: ArrayBufferView, offset: Int, length: Int)

    actual fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: IntArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: LongArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: IoBuffer, length: Int): Int

    fun readAvailable(dst: Int8Array, offset: Int, length: Int): Int
    fun readAvailable(dst: ArrayBuffer, offset: Int, length: Int): Int
    fun readAvailable(dst: ArrayBufferView, offset: Int, length: Int): Int

    /*
     * Returns next byte (unsigned) or `-1` if no more bytes available
     */
    actual fun tryPeek(): Int

    /**
     * Copy available bytes to the specified [buffer] but keep them available.
     * If the underlying implementation could trigger
     * bytes population from the underlying source and block until any bytes available
     *
     * Very similar to [readAvailable] but don't discard copied bytes.
     *
     * @return number of bytes were copied
     */
    actual fun peekTo(buffer: IoBuffer): Int

    actual fun discard(n: Long): Long

    actual override fun close()
}
