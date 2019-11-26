@file:Suppress("NOTHING_TO_INLINE", "IntroduceWhenSubject")

package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlin.native.concurrent.*

public actual class Buffer constructor(
    val pointer: CPointer<ByteVar>,
    actual inline val size: Int
) {
    init {
        requirePositiveIndex(size, "size")
    }

    public actual inline fun loadByteAt(index: Int): Byte = pointer[assertIndex(index, 1)]

    public actual inline fun storeByteAt(index: Int, value: Byte) {
        pointer[assertIndex(index, 1)] = value
    }

    public actual companion object {
        @SharedImmutable
        public actual val EMPTY: Buffer = Buffer(nativeHeap.allocArray(0), 0)
    }
}

internal actual fun bufferOf(array: ByteArray, start: Int, end: Int): Buffer {
    TODO("Not yet implemented")
}