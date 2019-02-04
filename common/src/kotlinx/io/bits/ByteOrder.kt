package kotlinx.io.bits

/**
 * Reverse number's byte order
 */
expect fun Short.reverseByteOrder(): Short

/**
 * Reverse number's byte order
 */
expect fun Int.reverseByteOrder(): Int

/**
 * Reverse number's byte order
 */
expect fun Long.reverseByteOrder(): Long

/**
 * Reverse number's byte order
 */
expect fun Float.reverseByteOrder(): Float

/**
 * Reverse number's byte order
 */
expect fun Double.reverseByteOrder(): Double

/**
 * Reverse number's byte order
 */
@ExperimentalUnsignedTypes
fun UShort.reverseByteOrder(): UShort = toShort().reverseByteOrder().toUShort()

/**
 * Reverse number's byte order
 */
@ExperimentalUnsignedTypes
fun UInt.reverseByteOrder(): UInt = toInt().reverseByteOrder().toUInt()

/**
 * Reverse number's byte order
 */
@ExperimentalUnsignedTypes
fun ULong.reverseByteOrder(): ULong = toLong().reverseByteOrder().toULong()


