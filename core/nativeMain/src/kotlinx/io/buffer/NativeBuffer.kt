@file:Suppress("NOTHING_TO_INLINE", "IntroduceWhenSubject")

package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlinx.io.bits.internal.utils.*

public actual class Buffer constructor(val pointer: CPointer<ByteVar>, actual inline val size: Int) {
    init {
        requirePositiveIndex(size, "size")
    }

    public actual inline fun loadByteAt(index: Int): Byte = pointer[assertIndex(index, 1)]

    public actual inline fun storeByteAt(index: Int, value: Byte) {
        pointer[assertIndex(index, 1)] = value
    }

    public actual companion object {
        public actual val Empty: Buffer = Buffer(nativeHeap.allocArray(0), 0)
    }
}
