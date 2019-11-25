package kotlinx.io

//TODO decide what to use: Int, Long or UInt
typealias BinarySize = Int

/**
 * A generic representation of reusable input.
 */
interface Binary {
    /**
     * The size of the input in bytes or [INFINITE] in case the size could not be estimated
     */
    val size: BinarySize

    /**
     * Open the input, read and transform it to given result type then close it.
     * This method ensures input is closed properly in case of exception inside and prevents leaking of input.
     */
    fun <R> read(reader: Input.() -> R): R

    companion object {
        /**
         * Designates that the binary does not have fixed size, but instead is read until EOF
         */
        val INFINITE: BinarySize = BinarySize.MAX_VALUE
    }
}

/**
 * A Binary with random access functionality
 */
interface RandomAccessBinary : Binary {
    /**
     * Read at most [atMost] bytes starting at [from] offset from the beginning of the binary.
     * This method could be called multiple times simultaneously.
     *
     */
    fun <R> read(from: BinarySize, atMost: BinarySize = Binary.INFINITE, block: Input.() -> R): R

    override fun <R> read(reader: Input.() -> R): R = read(0, BinarySize.MAX_VALUE, reader)
}

/**
 * A special case for empty binary. There is not input associated with the binary.
 * It throws [EOFException] on any attempt of read.
 */
object EmptyBinary: RandomAccessBinary{
    override val size: BinarySize = 0

    override fun <R> read(from: BinarySize, atMost: BinarySize, block: Input.() -> R): R {
        throw EOFException("Reading from empty binary")
    }
}

//TODO Add basic RandomAccessBinary implementation that wraps regular binary