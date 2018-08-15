package kotlinx.io.core

import kotlinx.cinterop.*

actual interface Output : Appendable, Closeable {
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

//    fun writeFully(src: CPointer<ByteVar>, offset: Int, length: Int)
//    fun writeFully(src: CPointer<ByteVar>, offset: Long, length: Long)

    actual fun append(csq: CharArray, start: Int, end: Int): Appendable

    actual fun fill(n: Long, v: Byte)
    actual fun flush()
    actual override fun close()

    @Deprecated("Non-public API. Use writeWhile instead", level = DeprecationLevel.ERROR)
    actual fun `$prepareWrite$`(n: Int): IoBuffer

    @Deprecated("Non-public API. Use writeWhile instead", level = DeprecationLevel.ERROR)
    actual fun `$afterWrite$`()
}

fun Output.writeFully(src: ByteArray) {
    writeFully(src, 0, src.size)
}

fun Output.writeFully(src: ShortArray) {
    writeFully(src, 0, src.size)
}

fun Output.writeFully(src: IntArray) {
    writeFully(src, 0, src.size)
}

fun Output.writeFully(src: LongArray) {
    writeFully(src, 0, src.size)
}

fun Output.writeFully(src: FloatArray) {
    writeFully(src, 0, src.size)
}

fun Output.writeFully(src: DoubleArray) {
    writeFully(src, 0, src.size)
}

fun Output.writeFully(src: IoBuffer) {
    writeFully(src, src.readRemaining)
}

fun Output.fill(n: Long) {
    fill(n, 0)
}
