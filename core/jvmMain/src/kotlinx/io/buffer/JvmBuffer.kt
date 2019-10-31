@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import java.nio.*
import java.nio.ByteOrder

@Suppress("ACTUAL_WITHOUT_EXPECT")
public actual inline class Buffer(val buffer: ByteBuffer) {
    public actual inline val size: Int get() = buffer.limit()

    public actual inline fun loadByteAt(index: Int): Byte = buffer.get(index)

    public actual inline fun storeByteAt(index: Int, value: Byte) {
        buffer.put(index, value)
    }

    public actual companion object {
        public actual val Empty: Buffer = Buffer(ByteBuffer.allocate(0).order(ByteOrder.BIG_ENDIAN))
    }
}
