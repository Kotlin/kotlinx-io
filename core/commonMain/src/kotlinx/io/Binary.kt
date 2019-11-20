package kotlinx.io

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
        val INFINITE = BinarySize.MAX_VALUE
    }
}