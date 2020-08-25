package kotlinx.io.buffer

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.ArrayBufferView
import org.khronos.webgl.DataView
import org.khronos.webgl.Int8Array
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 * Copying bytes from a memory to itself is allowed.
 */
public actual fun Buffer.copyTo(
    destination: Buffer,
    offset: Int,
    length: Int,
    destinationOffset: Int
) {
    val src = Int8Array(view.buffer, view.byteOffset + offset, length)
    val dst = Int8Array(destination.view.buffer, destination.view.byteOffset + destinationOffset, length)

    dst.set(src)
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public actual fun Buffer.copyTo(
    destination: ByteArray,
    offset: Int,
    length: Int,
    destinationOffset: Int
) {
    @Suppress("UnsafeCastFromDynamic")
    val to: Int8Array = destination.asDynamic()

    val from = Int8Array(view.buffer, view.byteOffset + offset, length)

    to.set(from, destinationOffset)
}

/**
 * Fills memory range starting at the specified [offset] with [value] repeated [count] times.
 */
public actual fun Buffer.fill(offset: Int, count: Int, value: Byte) {
    for (index in offset until offset + count) {
        this[index] = value
    }
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public fun Buffer.copyTo(destination: ArrayBuffer, offset: Int, length: Int, destinationOffset: Int) {
    @Suppress("UnsafeCastFromDynamic")
    val to = Int8Array(destination, destinationOffset, length)
    val from = Int8Array(view.buffer, view.byteOffset + offset, length)

    to.set(from, 0)
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public fun Buffer.copyTo(destination: ArrayBufferView, offset: Int, length: Int, destinationOffset: Int) {
    @Suppress("UnsafeCastFromDynamic")
    val to = Int8Array(destination.buffer, destinationOffset + destination.byteOffset, length)
    val from = Int8Array(view.buffer, view.byteOffset + offset, length)

    to.set(from, 0)
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public fun ArrayBuffer.copyTo(destination: Buffer, offset: Int, length: Int, destinationOffset: Int) {
    val from = Int8Array(this, offset, length)
    val to = Int8Array(destination.view.buffer, destination.view.byteOffset + destinationOffset, length)

    to.set(from, 0)
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public fun ArrayBufferView.copyTo(destination: Buffer, offset: Int, length: Int, destinationOffset: Int) {
    buffer.copyTo(destination, offset + byteOffset, length, destinationOffset)
}

internal val Buffer.Int8ArrayView: Int8Array get() = Int8Array(view.buffer, view.byteOffset, view.byteLength)

/**
 * Executes the given [block] of code providing a temporary instance of [Buffer] view of this byte array range
 * starting at the specified [offset] and having the specified bytes [length].
 * By default, if neither [offset] nor [length] specified, the whole array is used.
 * An instance of [Buffer] provided into the [block] should be never captured and used outside of lambda.
 */
public actual inline fun <R> ByteArray.useBuffer(offset: Int, length: Int, block: (Buffer) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return Buffer.of(this, offset, length).let(block)
}

/**
 * Creates the [Buffer] view for the specified [view].
 */
public fun Buffer.Companion.of(view: DataView): Buffer = Buffer(view)

/**
 * Creates the [Buffer] view for the specified [buffer] range starting at [offset] and the specified bytes [length].
 */
public fun Buffer.Companion.of(buffer: ArrayBuffer, offset: Int = 0, length: Int = buffer.byteLength - offset): Buffer {
    return Buffer.of(DataView(buffer, offset, length))
}

/**
 * Creates the [Buffer] view for the specified [array] range starting at [offset] and the specified bytes [length].
 */
public fun Buffer.Companion.of(array: ByteArray, offset: Int = 0, length: Int = array.size - offset): Buffer {
    @Suppress("UnsafeCastFromDynamic")
    val typedArray: Int8Array = array.asDynamic()
    return Buffer.of(typedArray, offset, length)
}

/**
 * Creates the [Buffer] view for the specified [view] range starting at [offset] and the specified bytes [length].
 */
public fun Buffer.Companion.of(view: ArrayBufferView, offset: Int = 0, length: Int = view.byteLength): Buffer {
    return Buffer.of(view.buffer, view.byteOffset + offset, length)
}

/**
 * Compacts the [Buffer]. Moves the of the buffer content from [startIndex] to [endIndex] range to the beginning of the buffer.
 * The copying ranges can overlap.
 *
 * @return [endIndex] - [startIndex] (copied bytes count) or updated [endIndex]
 */
internal actual fun Buffer.compact(startIndex: Int, endIndex: Int): Int {
    if (startIndex == 0) {
        return endIndex
    }
    @Suppress("UNUSED_VARIABLE")
    val array = Int8ArrayView
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/TypedArray/copyWithin
    js("array.copyWithin(0, startIndex, endIndex)")
    return endIndex - startIndex
}
