package kotlinx.io.core

import java.nio.*

/**
 * This shouldn't be implemented directly. Inherit [AbstractOutput] instead.
 */
actual interface Output : Closeable, Appendable {
    actual var byteOrder: ByteOrder

    actual fun writeByte(v: Byte)
    actual fun writeShort(v: Short)
    actual fun writeInt(v: Int)
    actual fun writeLong(v: Long)
    actual fun writeFloat(v: Float)
    actual fun writeDouble(v: Double)

    actual fun writeFully(src: ByteArray, offset: Int, length: Int)
    actual fun writeFully(src: ShortArray, offset: Int, length: Int)
    actual fun writeFully(src: IntArray, offset: Int, length: Int)
    actual fun writeFully(src: LongArray, offset: Int, length: Int)
    actual fun writeFully(src: FloatArray, offset: Int, length: Int)
    actual fun writeFully(src: DoubleArray, offset: Int, length: Int)
    actual fun writeFully(src: IoBuffer, length: Int)

    actual fun append(csq: CharArray, start: Int, end: Int): Appendable

    fun writeFully(bb: ByteBuffer)

    actual fun fill(n: Long, v: Byte)
    actual fun flush()
    actual override fun close()
}

