@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import org.khronos.webgl.*

public actual class Buffer(public val view: DataView) {

    public actual inline val size: Int get() = view.byteLength

    public actual fun loadByteAt(index: Int): Byte = checked(index) {
        return view.getInt8(index)
    }

    public actual fun storeByteAt(index: Int, value: Byte) = checked(index) {
        view.setInt8(index, value)
    }

    public actual companion object {
        public actual val EMPTY: Buffer = Buffer(DataView(ArrayBuffer(0)))
    }
}

internal actual fun bufferOf(array: ByteArray, start: Int, end: Int): Buffer {
    val uint8Array = array as Uint8Array
    val view = DataView(uint8Array.buffer, uint8Array.byteOffset + start, end - start)
    return Buffer(view)
}