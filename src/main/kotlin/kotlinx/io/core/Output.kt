package kotlinx.io.core

expect interface Output : Appendable {
    var byteOrder: ByteOrder

    fun writeByte(v: Byte)
    fun writeShort(v: Short)
    fun writeInt(v: Int)
    fun writeLong(v: Long)
    fun writeFloat(v: Float)
    fun writeDouble(v: Double)

    fun writeFully(src: ByteArray, offset: Int, length: Int)
    fun writeFully(src: ShortArray, offset: Int, length: Int)
    fun writeFully(src: IntArray, offset: Int, length: Int)
    fun writeFully(src: LongArray, offset: Int, length: Int)
    fun writeFully(src: FloatArray, offset: Int, length: Int)
    fun writeFully(src: DoubleArray, offset: Int, length: Int)
    fun writeFully(src: BufferView, length: Int)

    fun append(csq: CharArray, start: Int, end: Int): Appendable

    fun fill(n: Long, v: Byte)

    fun flush()
    fun close()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.append(csq: CharSequence, start: Int = 0, end: Int = csq.length): Appendable {
    return append(csq, start, end)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.append(csq: CharArray, start: Int = 0, end: Int = csq.size): Appendable {
    return append(csq, start, end)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: ByteArray, offset: Int = 0, length: Int = src.size) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: ShortArray, offset: Int = 0, length: Int = src.size) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: IntArray, offset: Int = 0, length: Int = src.size) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: LongArray, offset: Int = 0, length: Int = src.size) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: FloatArray, offset: Int = 0, length: Int = src.size) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: DoubleArray, offset: Int = 0, length: Int = src.size) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: BufferView, length: Int = src.readRemaining) {
    writeFully(src, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.fill(n: Long, v: Byte = 0) {
    fill(n, v)
}