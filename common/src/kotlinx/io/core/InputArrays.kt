package kotlinx.io.core

import kotlinx.io.core.internal.*

fun Input.peekTo(buffer: Buffer): Int {
    if (this is AbstractInput) {
        var copied = 0
        var chunk = head
        val dst = buffer.memory
        var dstPosition = buffer.writePosition
        var remaining = buffer.writeRemaining

        while (remaining > 0) {
            val size = minOf(remaining, chunk.readRemaining)
            chunk.memory.copyTo(dst, chunk.readPosition, size, dstPosition)
            dstPosition += size
            copied += size
            remaining -= size

            chunk = chunk.next ?: break
        }

        buffer.commitWritten(copied)
        return copied
    }

    return peekToFallback(buffer)
}

private fun Input.peekToFallback(buffer: Buffer): Int {
    val head = prepareReadFirstHead(1) ?: return 0
    val size = minOf(buffer.writeRemaining, head.readRemaining)
    head.memory.copyTo(buffer.memory, head.readPosition, size, buffer.writePosition)
    buffer.commitWritten(size)
    return size
}

fun Input.peekTo(min: Int, buffer: Buffer): Int {
    if (!prefetch(min)) prematureEndOfStream(min)
    val size = peekTo(buffer)
    if (size < min) prematureEndOfStream(min)
    return size
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyBytesTemplate(offset, length) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }.requireNoRemaining()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: ShortArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyTemplate(offset, length, 2) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }.requireNoRemaining()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: IntArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyTemplate(offset, length, 4) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }.requireNoRemaining()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: LongArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyTemplate(offset, length, 8) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }.requireNoRemaining()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: FloatArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyTemplate(offset, length, 4) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }.requireNoRemaining()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: DoubleArray, offset: Int = 0, length: Int = dst.size - offset) {
    readFullyTemplate(offset, length, 8) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }.requireNoRemaining()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: Buffer, length: Int = dst.writeRemaining) {
    readFullyBytesTemplate(0, length) { src, _, count ->
        src.readFully(dst, count)
    }.requireNoRemaining()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return length - readFullyBytesTemplate(offset, length) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: ShortArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return length - readFullyTemplate(offset, length, 2) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: IntArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return length - readFullyTemplate(offset, length, 4) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: LongArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return length - readFullyTemplate(offset, length, 8) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: FloatArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return length - readFullyTemplate(offset, length, 4) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: DoubleArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return length - readFullyTemplate(offset, length, 8) { src, dstOffset, count ->
        src.readFully(dst, dstOffset, count)
    }
}

fun Input.readAvailable(dst: Buffer, length: Int = dst.writeRemaining): Int {
    return length - readFullyBytesTemplate(0, length) { src, _, count ->
        src.readFully(dst, count)
    }
}

/**
 * @return number of bytes remaining or 0 if all [length] bytes were copied
 */
private inline fun Input.readFullyBytesTemplate(
    offset: Int,
    length: Int,
    readBlock: (src: Buffer, dstOffset: Int, count: Int) -> Unit
): Int {
    var remaining = length
    var dstOffset = offset

    takeWhile { buffer ->
        val count = minOf(remaining, buffer.readRemaining)
        readBlock(buffer, dstOffset, count)
        remaining -= count
        dstOffset += count

        remaining > 0
    }

    return remaining
}

/**
 * @return number of elements remaining or 0 if all [length] elements were copied
 */
private inline fun Input.readFullyTemplate(
    offset: Int,
    length: Int,
    componentSize: Int,
    readBlock: (src: Buffer, dstOffset: Int, count: Int) -> Unit
): Int {
    var remaining = length
    var dstOffset = offset

    takeWhileSize { buffer ->
        val count = minOf(remaining, buffer.readRemaining / componentSize)
        readBlock(buffer, dstOffset, count)
        remaining -= count
        dstOffset += count

        when {
            remaining > 0 -> componentSize
            else -> 0
        }
    }

    return remaining
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.requireNoRemaining() {
    if (this > 0) {
        prematureEndOfStream(this)
    }
}
