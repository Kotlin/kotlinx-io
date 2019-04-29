@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import org.khronos.webgl.*

/**
 * Represents a linear range of bytes.
 */
actual class Buffer(val view: DataView) {
    /**
     * Size of memory range in bytes.
     */
    actual inline val size: Int get() = view.byteLength

    /**
     * Returns byte at [index] position.
     */
    actual inline fun loadByteAt(index: Int): Byte {
        return view.getInt8(index)
    }

    /**
     * Write [value] at the specified [index].
     */
    actual inline fun storeByteAt(index: Int, value: Byte) {
        view.setInt8(index, value)
    }


    actual companion object {
        /**
         * Represents an empty memory region
         */
        actual val Empty: Buffer = Buffer(DataView(ArrayBuffer(0)))
    }
}
