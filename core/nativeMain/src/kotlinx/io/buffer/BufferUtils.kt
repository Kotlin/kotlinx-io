package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlinx.io.bits.internal.utils.*
import platform.posix.*

@PublishedApi
internal const val unalignedAccessSupported = UNALIGNED_ACCESS_ALLOWED == 1

@PublishedApi
internal fun Buffer.assertIndex(offset: Int, valueSize: Int): Int {
    assert(offset >= 0 && offset <= size - valueSize) {
        throw IndexOutOfBoundsException("offset $offset outside of range [0; ${size - valueSize})")
    }
    return offset
}

@PublishedApi
internal fun Buffer.assertIndex(offset: Long, valueSize: Long): Int {
    assert(offset >= 0 && offset <= size - valueSize) {
        throw IndexOutOfBoundsException("offset $offset outside of range [0; ${size - valueSize})")
    }
    return offset.toInt()
}

internal fun requirePositiveIndex(value: Int, name: String) {
    if (value < 0) {
        throw IndexOutOfBoundsException("$name shouldn't be negative: $value")
    }
}

internal fun requireRange(offset: Int, length: Int, size: Int, name: String) {
    if (offset + length > size) {
        throw IndexOutOfBoundsException("Wrong offset/count for $name: offset $offset, length $length, size $size")
    }
}

internal fun copy(
    source: IntArray,
    destinationPointer: CPointer<ByteVar>,
    sourceOffset: Int,
    count: Int
) {
    source.usePinned { pinned ->
        memcpy(destinationPointer, pinned.addressOf(sourceOffset), (count * 4L).convert())
    }
}

internal fun copy(
    source: ShortArray,
    destinationPointer: CPointer<ByteVar>,
    sourceOffset: Int,
    count: Int
) {
    source.usePinned { pinned ->
        memcpy(destinationPointer, pinned.addressOf(sourceOffset), (count * 2L).convert())
    }
}

internal fun copy(
    source: LongArray,
    destinationPointer: CPointer<ByteVar>,
    sourceOffset: Int,
    count: Int
) {
    source.usePinned { pinned ->
        memcpy(destinationPointer, pinned.addressOf(sourceOffset), (count * 8L).convert())
    }
}

internal fun copy(
    source: FloatArray,
    destinationPointer: CPointer<ByteVar>,
    sourceOffset: Int,
    count: Int
) {
    source.usePinned { pinned ->
        memcpy(destinationPointer, pinned.addressOf(sourceOffset), (count * 4L).convert())
    }
}

internal fun copy(
    source: DoubleArray,
    destinationPointer: CPointer<ByteVar>,
    sourceOffset: Int,
    count: Int
) {
    source.usePinned { pinned ->
        memcpy(destinationPointer, pinned.addressOf(sourceOffset), (count * 8L).convert())
    }
}

