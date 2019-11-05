package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlinx.io.bits.internal.utils.*
import platform.posix.*
import kotlin.contracts.*

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
internal  fun Buffer.assertIndex(offset: Long, valueSize: Long): Long {
    assert(offset >= 0 && offset <= size - valueSize) {
        throw IndexOutOfBoundsException("offset $offset outside of range [0; ${size - valueSize})")
    }
    return offset
}

internal fun requirePositiveIndex(value: Int, name: String) {
    if (value < 0) {
        throw IndexOutOfBoundsException("$name shouldn't be negative: $value")
    }
}

internal inline fun requireRange(offset: Int, length: Int, size: Int, name: String) {
    if (offset + length > size) {
        throw IndexOutOfBoundsException("Wrong offset/count for $name: offset $offset, length $length, size $size")
    }
}

internal inline fun Buffer.isAlignedShort(offset: Int) = (pointer.toLong() + offset) and 1L == 0L
internal inline fun Buffer.isAlignedInt(offset: Int) = (pointer.toLong() + offset) and 11L == 0L
internal inline fun Buffer.isAlignedLong(offset: Int) = (pointer.toLong() + offset) and 111L == 0L


internal inline fun storeArrayIndicesCheck(
    offset: Int,
    sourceOffset: Int,
    count: Int,
    itemSize: Int,
    sourceSize: Int,
    memorySize: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(sourceOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    requireRange(sourceOffset, count, sourceSize, "source")
    requireRange(offset, count * itemSize, memorySize, "memory")
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

