package kotlinx.io

/**
 * ByteOrder is the enumeration that represents an endianness of the arbitrary binary data.
 * Endianness refers to the order of bytes within a binary representation.
 */
public expect enum class ByteOrder {
    /**
     * Big-endian: the most significant byte is at the lowest address.
     */
    BIG_ENDIAN,

    /**
     * Little-endian: the least significant byte is at the lowest address.
     */
    LITTLE_ENDIAN;

    public companion object {
        /**
         * The byte order of the underlying platform.
         */
        public val native: ByteOrder
    }
}

internal expect fun Long.reverseByteOrder(): Long
