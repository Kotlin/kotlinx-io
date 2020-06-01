package kotlinx.io

import kotlin.math.min

private const val HEX_DIGIT_CHARS = "0123456789abcdef"
private const val BASE_64_DIGIT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

private operator fun CharSequence.component1(): Char = this[0]
private operator fun CharSequence.component2(): Char = this[1]
private operator fun CharSequence.component3(): Char? = getOrNull(2)
private operator fun CharSequence.component4(): Char? = getOrNull(3)

/**
 * An immutable sequence of bytes that can be read multiple times.
 */
abstract class Binary : Iterable<Byte> {

    protected var cachedHashCode: Int = 0

    /**
     * The size of the sequence of bytes this [Binary] represents
     */
    abstract val size: Int

    /**
     * This [Binary] encoded as a hexidecimal string.
     *
     * This calculation is lazy and will be cached after the first access to this property.
     */
    val hex: String by lazy {
        val array = CharArray(size * 2)

        var index = 0
        for (byte in this) {
            array[index++] = HEX_DIGIT_CHARS[(byte.toInt() shr 4) and 0xf]
            array[index++] = HEX_DIGIT_CHARS[byte.toInt() and 0xf]
        }

        String(array)
    }

    /**
     * Encode this [Binary] as a [Base64](http://www.ietf.org/rfc/rfc2045.txt) text.
     * This implementation omits newline delimiters.
     *
     * This calculation is lazy and will be cached after the first access to this property.
     */
    val base64: String by lazy {
        val chars = CharArray(size = (size + 2) / 3 * 4)
        var index = 0

        // grab in increments of 3 bytes for 24 bits total.
        for (binary in chunked(size = 3)) {
            when (binary.size) {
                3 -> {
                    val first = binary[0].toInt() shl 16
                    val second = binary[1].toInt() shl 8
                    val third = binary[2].toInt()

                    val accumulator = first + second + third

                    chars[index++] = BASE_64_DIGIT_CHARS[(accumulator shr 18) and 0x3f]
                    chars[index++] = BASE_64_DIGIT_CHARS[(accumulator shr 12) and 0x3f]
                    chars[index++] = BASE_64_DIGIT_CHARS[(accumulator shr 6) and 0x3f]
                    chars[index++] = BASE_64_DIGIT_CHARS[accumulator and 0x3f]
                }
                2 -> {
                    val first = binary[0].toInt() shl 8
                    val second = binary[1].toInt()

                    val accumulator = (first + second) shl 2 // align to 18 bits

                    chars[index++] = BASE_64_DIGIT_CHARS[(accumulator shr 12) and 0x3f]
                    chars[index++] = BASE_64_DIGIT_CHARS[(accumulator shr 6) and 0x3f]
                    chars[index++] = BASE_64_DIGIT_CHARS[accumulator and 0x3f]
                    chars[index++] = '='
                }
                1 -> {
                    val first = binary[0].toInt()

                    val accumulator = first shl 4 // align to 12 bits

                    chars[index++] = BASE_64_DIGIT_CHARS[(accumulator shr 6) and 0x3f]
                    chars[index++] = BASE_64_DIGIT_CHARS[accumulator and 0x3f]
                    chars[index++] = '='
                    chars[index++] = '='
                }
            }
        }

        String(chars)
    }

    /**
     * Decode this [Binary] as UTF-8 text.
     *
     * This calculation is lazy and will be cached after the first access to this property.
     */
    val utf8: String by lazy { TODO() }

    /**
     * Calculate the MD5 hash of this [Binary].
     *
     * This calculation is lazy and will be cached after the first access to this property.
     */
    val md5: String by lazy { TODO() }

    /**
     * Calculate the SHA1 hash of this [Binary].
     *
     * This calculation is lazy and will be cached after the first access to this property.
     */
    val sha1: String by lazy { TODO() }

    val sha256: String by lazy { TODO() }

    val sha512: String by lazy { TODO() }

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

    /**
     * By default, generates a copy of the underlying bytes of this [Binary]
     * and returns the result of that [ByteArray.contentHashCode].
     *
     * This operation is cached.
     */
    override fun hashCode(): Int {
        if (cachedHashCode == 0) {
            cachedHashCode = toByteArray().contentHashCode()
        }

        return cachedHashCode
    }

    companion object {

        /**
         * Decode the value represented by [hex] into a [Binary].
         */
        fun fromHexString(hex: String): Binary {
            require(hex.length % 2 == 0) { "Expected to be byte aligned but has length ${hex.length}" }
            return hex.toLowerCase().chunked(2) { (first, second) ->
                require(first in HEX_DIGIT_CHARS && second in HEX_DIGIT_CHARS) {
                    "Not a hex character 0x$first$second"
                }

                ((HEX_DIGIT_CHARS.indexOf(first) shl 4) or HEX_DIGIT_CHARS.indexOf(second)).toByte()
            }.toByteArray().asBinary()
        }

        /**
         * Decode the Base64 encoded string [base64] into a [Binary] object.
         */
        fun fromBase64(base64: String): Binary {
            require(base64.length % 4 == 0) { "Expected input to be 4 byte aligned but has length ${base64.length}" }
            val stripped = base64.filter { it != '=' }
            require(stripped.all { it in BASE_64_DIGIT_CHARS }) { "Illegal character in input $base64" }

            val size = ((stripped.length / 4) * 3) + when (base64.length - stripped.length) {
                0 -> 0
                1 -> 2
                2 -> 1
                else -> error("Stripped more than two padding characters")
            }

            var index = 0
            val array = ByteArray(size)
            for ((first, second, third, fourth) in stripped.chunked(size = 4)) {
                when {
                    third == null && fourth == null -> {
                        val firstIndex = BASE_64_DIGIT_CHARS.indexOf(first) and 0x3f shl 6
                        val secondIndex = BASE_64_DIGIT_CHARS.indexOf(second) and 0x3f

                        val accumulate = firstIndex + secondIndex

                        array[index++] = (accumulate shr 4).toByte()
                    }
                    third != null && fourth == null -> {
                        val firstIndex = BASE_64_DIGIT_CHARS.indexOf(first) and 0x3f shl 12
                        val secondIndex = BASE_64_DIGIT_CHARS.indexOf(second) and 0x3f shl 6
                        val thirdIndex = BASE_64_DIGIT_CHARS.indexOf(third) and 0x3f

                        val accumulate = firstIndex + secondIndex + thirdIndex

                        array[index++] = ((accumulate and 0x3fc00) shr 10).toByte()
                        array[index++] = ((accumulate and 0x3fc) shr 2).toByte()
                    }
                    third != null && fourth != null -> {
                        val firstIndex = BASE_64_DIGIT_CHARS.indexOf(first) and 0x3f shl 18
                        val secondIndex = BASE_64_DIGIT_CHARS.indexOf(second) and 0x3f shl 12
                        val thirdIndex = BASE_64_DIGIT_CHARS.indexOf(third) and 0x3f shl 6
                        val fourthIndex = BASE_64_DIGIT_CHARS.indexOf(fourth) and 0x3f

                        val accumulate = firstIndex + secondIndex + thirdIndex + fourthIndex

                        array[index++] = (accumulate shr 16 and 0xff).toByte()
                        array[index++] = (accumulate shr 8 and 0xff).toByte()
                        array[index++] = (accumulate and 0xff).toByte()
                    }
                }
            }

            return array.asBinary(defensiveCopy = false)
        }
    }
}

/**
 * A [Binary] wrapper for a [ByteArray]. In order to ensure immutability, [ByteArrayBinary] has the ability
 * to perform a defensive copy.
 */
internal class ByteArrayBinary internal constructor(data: ByteArray, defensiveCopy: Boolean) : Binary() {

    private val data = if (defensiveCopy) data.copyOf() else data

    override val size = data.size

    override fun get(index: Int): Byte = data[index]

    override fun contains(byte: Byte): Boolean = byte in data

    override fun toByteArray(): ByteArray = data.copyOf()

    override fun equals(other: Any?): Boolean = when (other) {
        is ByteArrayBinary -> other.data.contentEquals(data)
        else -> super.equals(other)
    }

    override fun hashCode(): Int {
        if (cachedHashCode == 0) {
            cachedHashCode = data.contentHashCode()
        }

        return cachedHashCode
    }
}

/**
 * Get a [Binary] wrapper around [this]. In order to ensure immutability of the underlying resource,
 * you should set [defensiveCopy] to true.
 *
 * Note: Utility functions that are cached like [Binary.hex] are not guaranteed to be correct if a
 * defensive copy is not used and the underlying resource is mutated.
 */
fun ByteArray.asBinary(defensiveCopy: Boolean = true): Binary = ByteArrayBinary(this, defensiveCopy)

/**
 * A subset view into [this] where index 0 correspends to [startIndex] and the last index corresponds to
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
            if (currentByte == byte) return true
        }

        return false
    }
}

/**
 * Splits the data contained in [this] into [Binary] chunks of [size] bytes.
 * The final chunk of this list may be smaller than [size].
 *
 * This oprator does not allocate any new memory under the hood, and instead uses [slice]
 * to produce the chunks of data.
 */
fun Binary.chunked(size: Int): List<Binary> {
    val result = mutableListOf<Binary>()
    val lastIndex = if (this.size % size == 0) this.size else (this.size / size) + this.size

    for (startIndex in 0 until lastIndex step size) {
        if (startIndex > this.size) break
        val endIndex = min(startIndex + size - 1, this.size - 1)
        result.add(slice(startIndex, endIndex))
    }

    return result
}
