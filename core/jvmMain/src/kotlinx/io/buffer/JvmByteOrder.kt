@file:JvmName("ByteOrderJVMKt")

package kotlinx.io.buffer

actual enum class ByteOrder(val nioOrder: java.nio.ByteOrder) {
    BIG_ENDIAN(java.nio.ByteOrder.BIG_ENDIAN),
    LITTLE_ENDIAN(java.nio.ByteOrder.LITTLE_ENDIAN);

    actual companion object {
        actual val native: ByteOrder = orderOf(java.nio.ByteOrder.nativeOrder())
        fun of(nioOrder: java.nio.ByteOrder): ByteOrder = orderOf(nioOrder)
    }
}

private fun orderOf(nioOrder: java.nio.ByteOrder): ByteOrder = when (nioOrder) {
    java.nio.ByteOrder.BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
    else -> ByteOrder.LITTLE_ENDIAN
}

/**
 * Reverse number's byte order
 */
@Suppress("NOTHING_TO_INLINE")
actual inline fun Short.reverseByteOrder(): Short = java.lang.Short.reverseBytes(this)


/**
 * Reverse number's byte order
 */
@Suppress("NOTHING_TO_INLINE")
actual inline fun Int.reverseByteOrder(): Int = java.lang.Integer.reverseBytes(this)


/**
 * Reverse number's byte order
 */
@Suppress("NOTHING_TO_INLINE")
actual inline fun Long.reverseByteOrder(): Long = java.lang.Long.reverseBytes(this)


/**
 * Reverse number's byte order
 */
@Suppress("NOTHING_TO_INLINE")
actual inline fun Float.reverseByteOrder(): Float =
    java.lang.Float.intBitsToFloat(
        java.lang.Integer.reverseBytes(
            java.lang.Float.floatToRawIntBits(this)
        )
    )

/**
 * Reverse number's byte order
 */
@Suppress("NOTHING_TO_INLINE")
actual inline fun Double.reverseByteOrder(): Double =
    java.lang.Double.longBitsToDouble(
        java.lang.Long.reverseBytes(
            java.lang.Double.doubleToRawLongBits(this)
        )
    )

