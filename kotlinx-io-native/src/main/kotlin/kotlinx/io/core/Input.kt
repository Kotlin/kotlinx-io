package kotlinx.io.core

import kotlinx.cinterop.*
import kotlinx.io.core.internal.*

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

    fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int)
    fun readFully(dst: CPointer<ByteVar>, offset: Long, length: Long)

    actual fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: IntArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: LongArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int
    actual fun readAvailable(dst: IoBuffer, length: Int): Int

    fun readAvailable(dst: CPointer<ByteVar>, offset: Int, length: Int): Int
    fun readAvailable(dst: CPointer<ByteVar>, offset: Long, length: Long): Long

    /*
     * Returns next byte (unsigned) or `-1` if no more bytes available
     */
    actual fun tryPeek(): Int

    actual fun discard(n: Long): Long
    actual override fun close()

    @DangerousInternalIoApi
    actual fun `$updateRemaining$`(remaining: Int)

    @DangerousInternalIoApi
    actual fun `$ensureNext$`(current: IoBuffer): IoBuffer?

    @DangerousInternalIoApi
    actual fun `$prepareRead$`(minSize: Int): IoBuffer?
}

fun Input.readAvailable(dst: ByteArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: ShortArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: IntArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: LongArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: FloatArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: DoubleArray): Int = readAvailable(dst, 0, dst.size)

fun Input.readFully(dst: ByteArray) {
    readFully(dst, 0, dst.size)
}

fun Input.readFully(dst: ShortArray) {
    readFully(dst, 0, dst.size)
}

fun Input.readFully(dst: IntArray) {
    readFully(dst, 0,  dst.size)
}

fun Input.readFully(dst: LongArray) {
    readFully(dst, 0, dst.size)
}

fun Input.readFully(dst: FloatArray) {
    readFully(dst, 0, dst.size)
}

fun Input.readFully(dst: DoubleArray) {
    readFully(dst, 0, dst.size)
}
