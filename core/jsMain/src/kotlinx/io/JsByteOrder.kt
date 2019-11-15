@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io

import org.khronos.webgl.*

public actual enum class ByteOrder {
    BIG_ENDIAN,
    LITTLE_ENDIAN;

    public actual companion object {
        actual val native: ByteOrder = run {
            val buffer = ArrayBuffer(4)
            val arr = Int32Array(buffer)
            val view = DataView(buffer)
            arr[0] = 0x11223344
            if (view.getInt32(0, true) == 0x11223344) LITTLE_ENDIAN else BIG_ENDIAN
        }
    }
}

internal actual fun Long.reverseByteOrder(): Long = swap(this)

private inline fun swap(s: Short): Short = (((s.toInt() and 0xff) shl 8) or ((s.toInt() and 0xffff) ushr 8)).toShort()

private inline fun swap(s: Int): Int =
    (swap((s and 0xffff).toShort()).toInt() shl 16) or (swap((s ushr 16).toShort()).toInt() and 0xffff)

private inline fun swap(s: Long): Long =
    (swap((s and 0xffffffff).toInt()).toLong() shl 32) or (swap((s ushr 32).toInt()).toLong() and 0xffffffff)
