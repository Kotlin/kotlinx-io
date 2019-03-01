package kotlinx.io.core

import kotlinx.cinterop.*

@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")
actual interface Output : Appendable, Closeable {
    actual var byteOrder: ByteOrder

    actual fun writeByte(v: Byte)

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeShort(v: Short) {
        writeShort(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeInt(v: Int) {
        writeInt(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeLong(v: Long) {
        writeLong(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeFloat(v: Float) {
        writeFloat(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeDouble(v: Double) {
        writeDouble(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeFully(src: ByteArray, offset: Int, length: Int) {
        writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeFully(src: ShortArray, offset: Int, length: Int) {
        writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeFully(src: IntArray, offset: Int, length: Int) {
        writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeFully(src: LongArray, offset: Int, length: Int) {
        writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeFully(src: FloatArray, offset: Int, length: Int) {
        writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeFully(src: DoubleArray, offset: Int, length: Int) {
        writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Suppress("ACTUAL_WITHOUT_EXPECT")
    actual fun writeFully(src: IoBuffer, length: Int) {
        writeFully(src, length)
    }

    fun writeFully(src: CPointer<ByteVar>, offset: Int, length: Int)
    fun writeFully(src: CPointer<ByteVar>, offset: Long, length: Long)

    actual fun append(csq: CharArray, start: Int, end: Int): Appendable

    actual fun fill(n: Long, v: Byte)
    actual fun flush()
    actual override fun close()
}

