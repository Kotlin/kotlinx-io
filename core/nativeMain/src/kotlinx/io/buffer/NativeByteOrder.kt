@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import kotlinx.cinterop.*

actual enum class ByteOrder {
    BIG_ENDIAN, LITTLE_ENDIAN;

    actual companion object {
        actual val native: ByteOrder = memScoped {
            val i = alloc<IntVar>()
            i.value = 1
            val bytes = i.reinterpret<ByteVar>()
            if (bytes.value == 0.toByte()) BIG_ENDIAN else LITTLE_ENDIAN
        }
    }
}

/**
 * Reverse number's byte order
 */
actual fun Short.reverseByteOrder(): Short = swap(this)

/**
 * Reverse number's byte order
 */
actual fun Int.reverseByteOrder(): Int = swap(this)

/**
 * Reverse number's byte order
 */
actual fun Long.reverseByteOrder(): Long = swap(this)

/**
 * Reverse number's byte order
 */
actual fun Float.reverseByteOrder(): Float = swap(this)

/**
 * Reverse number's byte order
 */
actual fun Double.reverseByteOrder(): Double = swap(this)


private inline fun swap(s: Short): Short = (((s.toInt() and 0xff) shl 8) or ((s.toInt() and 0xffff) ushr 8)).toShort()

private inline fun swap(s: Int): Int =
    (swap((s and 0xffff).toShort()).toInt() shl 16) or (swap((s ushr 16).toShort()).toInt() and 0xffff)

private inline fun swap(s: Long): Long =
    (swap((s and 0xffffffff).toInt()).toLong() shl 32) or (swap((s ushr 32).toInt()).toLong() and 0xffffffff)

private inline fun swap(s: Float): Float = Float.fromBits(swap(s.toRawBits()))

private inline fun swap(s: Double): Double = Double.fromBits(swap(s.toRawBits()))

