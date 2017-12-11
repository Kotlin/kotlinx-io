package kotlinx.io.core

expect interface Input {
    var byteOrder: ByteOrder

    fun readByte(): Byte
    fun readShort(): Short
    fun readInt(): Int
    fun readLong(): Long
    fun readFloat(): Float
    fun readDouble(): Double

    fun readFully(dst: ByteArray, offset: Int, length: Int)
    fun readFully(dst: ShortArray, offset: Int, length: Int)
    fun readFully(dst: IntArray, offset: Int, length: Int)
    fun readFully(dst: LongArray, offset: Int, length: Int)
    fun readFully(dst: FloatArray, offset: Int, length: Int)
    fun readFully(dst: DoubleArray, offset: Int, length: Int)
    fun readFully(dst: BufferView, length: Int)

    fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int
    fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int
    fun readAvailable(dst: IntArray, offset: Int, length: Int): Int
    fun readAvailable(dst: LongArray, offset: Int, length: Int): Int
    fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int
    fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int
    fun readAvailable(dst: BufferView, length: Int): Int

    fun discard(n: Long): Long
}

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

fun Input.readAvailable(dst: ByteArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: ShortArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: IntArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: LongArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: FloatArray): Int = readAvailable(dst, 0, dst.size)
fun Input.readAvailable(dst: DoubleArray): Int = readAvailable(dst, 0, dst.size)

fun Input.discardExact(n: Long) {
    val discarded = discard(n)
    if (discarded != n) {
        throw IllegalStateException("Only $discarded bytes were discarded of $n requested")
    }
}

fun Input.discardExact(n: Int) {
    discardExact(n.toLong())
}