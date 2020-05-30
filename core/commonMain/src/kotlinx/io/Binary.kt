package kotlinx.io

import kotlin.math.min

/**
 * An immutable sequence of bytes that can be read multiple times.
 */
public interface Binary : Iterable<Byte> {

    /**
     * The size of the sequence of bytes this [Binary] represents
     */
    public val size: Int

    /**
     * Get a single byte at [index]. When requesting a byte at index outside of range
     * [0, size) throw [IndexOutOfBoundsException]
     */
    public operator fun get(index: Int): Byte

    /**
     * Checks if [byte] is contained in this [Binary]
     */
    public operator fun contains(byte: Byte): Boolean

    public override fun iterator(): Iterator<Byte> = object : Iterator<Byte> {

        private var currentIndex: Int = 0

        override fun hasNext(): Boolean = currentIndex < size

        override fun next(): Byte = this@Binary[currentIndex++]
    }

    public override fun equals(other: Any?): Boolean

    public override fun hashCode(): Int
}

internal class ByteArrayBinary internal constructor(data: ByteArray, defensiveCopy: Boolean) : Binary {

    private val data = if (defensiveCopy) data.copyOf() else data

    override val size = data.size

    override fun get(index: Int): Byte = data[index]

    override fun contains(byte: Byte): Boolean = byte in data

    override fun equals(other: Any?): Boolean = when (other) {
        is ByteArrayBinary -> other.data.contentEquals(data)
        is Binary -> genericBinaryEquals(this, other)
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
public fun ByteArray.asBinary(defensiveCopy: Boolean = true): Binary = ByteArrayBinary(this, defensiveCopy)

/**
 * A view into a subset of [this] where index 0 correspends to [startIndex] and the last index corresponds to
 * [endIndex].
 */
public fun Binary.slice(startIndex: Int = 0, endIndex: Int = size - 1): Binary = object : Binary {

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

    override fun equals(other: Any?): Boolean = when (other) {
        is Binary -> genericBinaryEquals(this, other)
        else -> false
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
public fun Binary.chunked(chunkSize: Int): List<Binary> {
    val result = mutableListOf<Binary>()

    val lastIndex = if (size % chunkSize == 0) size else (size / chunkSize) + size

    for (startIndex in 0 until lastIndex step chunkSize) {
        if (startIndex > size) break
        val endIndex = min(startIndex + chunkSize - 1, size - 1)
        result.add(slice(startIndex, endIndex))
    }

    return result
}

private fun genericBinaryEquals(first: Binary, second: Binary): Boolean {
    if (first.size != second.size) return false

    first.forEachIndexed { index, byte ->
        if (byte != second[index]) return false
    }

    return true
}
