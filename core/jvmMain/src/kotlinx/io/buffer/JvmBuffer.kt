@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import java.nio.*
import java.nio.ByteOrder

@Suppress("ACTUAL_WITHOUT_EXPECT", "EXPERIMENTAL_FEATURE_WARNING")
actual inline class Buffer(val buffer: ByteBuffer) {
    /**
     * Size of buffer range in bytes.
     */
    actual inline val size: Int get() = buffer.limit()

    /**
     * Returns byte at [index] position.
     */
    actual inline fun loadByteAt(index: Int): Byte = buffer.get(index)

    /**
     * Write [value] at the specified [index]
     */
    actual inline fun storeByteAt(index: Int, value: Byte) {
        buffer.put(index, value)
    }

    actual companion object {
        actual val Empty: Buffer = Buffer(ByteBuffer.allocate(0).order(ByteOrder.BIG_ENDIAN))
    }
}
