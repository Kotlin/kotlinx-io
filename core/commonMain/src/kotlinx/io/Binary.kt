package kotlinx.io

/**
 * A generic representation of reusable input.
 */
interface Binary {
    /**
     * The size of the input in bytes or [INFINITE] in case the size could not be estimated
     */
    val size: Int

    /**
     * Open the input, read and transform it to given result type then close it.
     * This method ensures input is closed properly in case of exception inside and prevents leaking of input.
     */
    fun <R> read(reader: Input.() -> R): R

    companion object {
        /**
         * Designates that the binary does not have fixed size, but instead is read until EOF
         */
        const val INFINITE: Int = Int.MAX_VALUE
    }
}

/**
 * A convenience method to use [Input] as parameter instead of receiver. Useful for nested input reads.
 */
public inline fun <R> Binary.readIt(crossinline block: (Input) -> R): R = read{block(this)}

/**
 * A Binary with random access functionality
 */
@ExperimentalIoApi
interface RandomAccessBinary : Binary {
    /**
     * Read at most [atMost] bytes starting at [from] offset from the beginning of the binary.
     * This method could be called multiple times simultaneously.
     *
     */
    fun <R> read(from: Int, atMost: Int = Binary.INFINITE, block: Input.() -> R): R

    override fun <R> read(reader: Input.() -> R): R = read(0, Int.MAX_VALUE, reader)
}

/**
 * A special case for empty binary. There is not input associated with the binary.
 * It throws [EOFException] on any attempt of read.
 */
@ExperimentalIoApi
object EmptyBinary: RandomAccessBinary{
    override val size: Int = 0

    override fun <R> read(from: Int, atMost: Int, block: Input.() -> R): R {
        throw EOFException("Reading from empty binary")
    }
}

//TODO Add basic RandomAccessBinary implementation that wraps regular binary

/**
 * Convert given binary into [ByteArray]
 */
@ExperimentalIoApi
fun Binary.toByteArray(): ByteArray = read {
    ByteArray(size).also {
        readArray(it)
    }
}

/**
 * Convert fragment of [RandomAccessBinary] to [ByteArray]
 */
@ExperimentalIoApi
fun RandomAccessBinary.toByteArray(from: Int, size: Int): ByteArray = read(from, size) {
    ByteArray(size).also {
        readArray(it)
    }
}