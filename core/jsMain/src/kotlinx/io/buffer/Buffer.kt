@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.khronos.webgl.Int8Array


public actual class Buffer(public val view: DataView) {

    public actual inline val size: Int get() = view.byteLength

    public actual fun loadByteAt(index: Int): Byte = checked(index) {
        return view.getInt8(index)
    }

    public actual fun storeByteAt(index: Int, value: Byte) = checked(index) {
        view.setInt8(index, value)
    }

    actual companion object
}

public actual val EMPTY: Buffer = Buffer(DataView(ArrayBuffer(0)))

/**
 * Wrap [array] into [Buffer] from [startIndex] to [endIndex].
 */
internal actual fun bufferOf(array: ByteArray, startIndex: Int, endIndex: Int): Buffer {
    val content = array as Int8Array
    val view = DataView(
        content.buffer, content.byteOffset + startIndex, endIndex - startIndex
    )
    return Buffer(view)
}
