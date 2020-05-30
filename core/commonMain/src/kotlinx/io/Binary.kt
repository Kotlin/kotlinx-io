package kotlinx.io

import kotlin.math.min

/**
 * An immutable sequence of bytes that can be read multiple times.
 */
abstract class Binary : Iterable<Byte> {

    /**
     * The size of the sequence of bytes this [Binary] represents
     */
    abstract val size: Int

    val utf8: String by lazy {
        TODO()
    }

    val base64: String by lazy {
        TODO()
    }

    val md5: String by lazy {
        TODO()
    }

    val sha1: String by lazy {
        TODO()
    }

    val sha256: String by lazy {
        TODO()
    }

    val sha512: String by lazy {
        TODO()
    }

    /**
     * Get a single byte at [index]. When requesting a byte at index outside of range
     * [0, size) throw [IndexOutOfBoundsException]
     */
    abstract operator fun get(index: Int): Byte

    /**
     * Checks if [byte] is contained in this [Binary]
     */
     abstract operator fun contains(byte: Byte): Boolean

    /**
     * Returns a [ByteArray] containing a copy of the bytes contained within this [Binary]
     */
     open fun toByteArray(): ByteArray = ByteArray(size) { get(it) }

     override fun iterator(): Iterator<Byte> = object : Iterator<Byte> {

        private var currentIndex: Int = 0

        override fun hasNext(): Boolean = currentIndex < size

        override fun next(): Byte = this@Binary[currentIndex++]
    }

     override fun equals(other: Any?): Boolean {
         if (other !is Binary) return false
         if (size != other.size) return false

         forEachIndexed { index, byte ->
             if (byte != other[index]) return false
         }

         return true
     }

     abstract override fun hashCode(): Int
}

internal class ByteArrayBinary internal constructor(data: ByteArray, defensiveCopy: Boolean) : Binary() {

    private val data = if (defensiveCopy) data.copyOf() else data

    override val size = data.size

    override fun get(index: Int): Byte = data[index]

    override fun contains(byte: Byte): Boolean = byte in data

    override fun toByteArray(): ByteArray = data.copyOf()

    override fun equals(other: Any?): Boolean = when (other) {
        is ByteArrayBinary -> other.data.contentEquals(data)
        is Binary -> super.equals(other)
        else -> false
    }

    override fun hashCode(): Int {
        TODO("Not yet implemented")
    }
}

/**
 * Get a [Binary] wrapper around [this]. In order to ensure immutability of the underlying resource,
 * you should set [defensiveCopy] to true.
 */
 fun ByteArray.asBinary(defensiveCopy: Boolean = true): Binary = ByteArrayBinary(this, defensiveCopy)

/**
 * A view into a subset of [this] where index 0 correspends to [startIndex] and the last index corresponds to
 * [endIndex].
 */
 fun Binary.slice(startIndex: Int = 0, endIndex: Int = size - 1): Binary = object : Binary() {

    init {
        require(startIndex >= 0) { "startIndex shouldn't be negative but was $startIndex" }
        require(startIndex <= endIndex) { "endIndex should not be less than startIndex" }
        require(endIndex < this@slice.size) { "endIndex $endIndex is out of bounds" }
    }

    override val size: Int = endIndex - startIndex + 1

    override fun get(index: Int): Byte {
        require(index >= 0) { "index must not be negative but was $index" }
        require(index < size) { "index $index is out of bounds [0, $size)" }

        return this@slice[index + startIndex]
    }

    override fun contains(byte: Byte): Boolean {
        for (currentByte in this) {
            if (currentByte == byte) {
                return true
            }
        }

        return false
    }

    override fun hashCode(): Int {
        TODO("Not yet implemented")
    }
}

/**
 * Splits the data contained in [this] into [Binary] chunks of size [chunkSize].
 * The final chunk of this list may be smaller than [chunkSize].
 *
 * This oprator does not allocate any new memeory under the hood, and instead uses [slice]
 * to produce the chunks of data.
 */
 fun Binary.chunked(chunkSize: Int): List<Binary> {
    val result = mutableListOf<Binary>()

    val lastIndex = if (size % chunkSize == 0) size else (size / chunkSize) + size

    for (startIndex in 0 until lastIndex step chunkSize) {
        if (startIndex > size) break
        val endIndex = min(startIndex + chunkSize - 1, size - 1)
        result.add(slice(startIndex, endIndex))
    }

    return result
}
