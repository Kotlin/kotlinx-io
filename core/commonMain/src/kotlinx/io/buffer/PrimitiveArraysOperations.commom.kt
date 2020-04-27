@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

/**
 * Copies bytes from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset].
 */
public inline fun Buffer.loadByteArray(
    offset: Int,
    destination: ByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination, offset, count, destinationOffset)
}

/**
 * Copies unsigned shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset].
 * @param sourceOffset items
 */
public inline fun Buffer.storeByteArray(
    offset: Int,
    source: ByteArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    source.useBuffer(sourceOffset, count) { buffer ->
        buffer.copyTo(this, 0, count, offset)
    }
}
