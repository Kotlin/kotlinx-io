package kotlinx.io.core

import org.khronos.webgl.*

/**
 * Shouldn't be implemented directly. Inherit [AbstractInput] instead.
 */
@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")
actual interface Input : Closeable {
    @Deprecated(
        "Not supported anymore. All operations are big endian by default.",
        level = DeprecationLevel.ERROR
    )
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual var byteOrder: ByteOrder
        get() = ByteOrder.BIG_ENDIAN
        set(newValue) {
            if (newValue != ByteOrder.BIG_ENDIAN) {
                throw IllegalArgumentException("Only BIG_ENDIAN is supported")
            }
        }

    /**
     * It is `true` when it is known that no more bytes will be available. When it is `false` then this means that
     * it is not known yet or there are available bytes.
     * Please note that `false` value doesn't guarantee that there are available bytes so `readByte()` may fail.
     */
    actual val endOfInput: Boolean

    /**
     * Prefetch at least [min] bytes from the underlying source. May do nothing if there are already requested bytes
     * buffered or when the underlying source is already consumed entirely.
     * @return `true` if at least [min] bytes available of `false` when not enough bytes buffered and
     * no more pending bytes in the underlying source.
     */
    actual fun prefetch(min: Int): Boolean

    /**
     * Read the next upcoming byte
     * @throws EOFException if no more bytes available.
     */
    actual fun readByte(): Byte

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readShort(): Short {
        return readShort()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readInt(): Int {
        return readInt()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readLong(): Long {
        return readLong()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readFloat(): Float {
        return readFloat()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readDouble(): Double {
        return readDouble()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readFully(dst: ByteArray, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readFully(dst: ShortArray, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readFully(dst: IntArray, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readFully(dst: LongArray, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readFully(dst: FloatArray, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readFully(dst: DoubleArray, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT", "DEPRECATION")
    actual fun readFully(dst: IoBuffer, length: Int) {
        return readFully(dst, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readAvailable(dst: IntArray, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readAvailable(dst: LongArray, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT", "DEPRECATION")
    actual fun readAvailable(dst: IoBuffer, length: Int): Int {
        return readAvailable(dst, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun readFully(dst: Int8Array, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun readFully(dst: ArrayBuffer, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun readFully(dst: ArrayBufferView, offset: Int, length: Int) {
        return readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun readAvailable(dst: Int8Array, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun readAvailable(dst: ArrayBuffer, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun readAvailable(dst: ArrayBufferView, offset: Int, length: Int): Int {
        return readAvailable(dst, offset, length)
    }

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
    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("DEPRECATION", "ACTUAL_WITHOUT_EXPECT")
    actual fun peekTo(buffer: IoBuffer): Int {
        return peekTo(buffer)
    }

    actual fun discard(n: Long): Long

    actual override fun close()
}
