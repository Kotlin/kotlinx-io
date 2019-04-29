@file:Suppress("NOTHING_TO_INLINE", "IntroduceWhenSubject")

package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlinx.io.*
import kotlinx.io.bits.internal.utils.*
import platform.posix.*

actual class Buffer constructor(val pointer: CPointer<ByteVar>, actual inline val size: Int) {
    init {
        requirePositiveIndex(size, "size")
    }

    /**
     * Returns byte at [index] position.
     */
    actual inline fun loadByteAt(index: Int): Byte = pointer[assertIndex(index, 1)]

    /**
     * Write [value] at the specified [index].
     */
    actual inline fun storeByteAt(index: Int, value: Byte) {
        pointer[assertIndex(index, 1)] = value
    }
    
    actual companion object {
        actual val Empty: Buffer = Buffer(nativeHeap.allocArray(0), 0)
    }
}
