package kotlinx.io.browser

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.js

/** Constructs a resizable [ArrayBuffer] */
internal fun ArrayBuffer(
    length: Double,
    maxByteLength: Double,
): ArrayBuffer = js("new ArrayBuffer(length, { maxByteLength: maxByteLength })")

/** Resizes this [ArrayBuffer]. Throws a JavaScript `RangeError` if the new length exceeds the maximum. */
internal fun resize(
    buffer: ArrayBuffer,
    length: Double,
): Unit = js("buffer.resize(length)")

/** Like [ArrayBuffer.byteLength], but exposes the full [Double] value. */
internal fun getByteLength(buffer: ArrayBuffer): Double = js("buffer.byteLength")

/** Like [Int8Array.get], but exposes the full [Double] index. */
internal fun getByte(
    array: Int8Array,
    index: Double,
): Byte = js("array[index]")

/** Like [Int8Array.set], but exposes the full [Double] index. */
internal fun setByte(
    array: Int8Array,
    index: Double,
    byte: Byte,
): Unit = js("array[index] = byte")
