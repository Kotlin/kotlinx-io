package kotlinx.io.core

expect interface Output {
    var byteOrder: ByteOrder

    fun writeByte(v: Byte)
    fun writeShort(v: Short)
    fun writeInt(v: Int)
    fun writeLong(v: Long)
    fun writeFloat(v: Float)
    fun writeDouble(v: Double)

    fun writeFully(src: ByteArray, offset: Int = 0, length: Int = src.size)
    fun writeFully(src: ShortArray, offset: Int = 0, length: Int = src.size)
    fun writeFully(src: IntArray, offset: Int = 0, length: Int = src.size)
    fun writeFully(src: LongArray, offset: Int = 0, length: Int = src.size)
    fun writeFully(src: FloatArray, offset: Int = 0, length: Int = src.size)
    fun writeFully(src: DoubleArray, offset: Int = 0, length: Int = src.size)
    fun writeFully(src: BufferView, length: Int = src.readRemaining)

    fun fill(n: Long, v: Byte = 0)
}

