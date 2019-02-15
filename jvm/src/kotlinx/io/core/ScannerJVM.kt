package kotlinx.io.core

import java.nio.*

internal actual fun Buffer.discardUntilDelimiterImpl(delimiter: Byte): Int {
    val bb = readBuffer
    return if (bb.hasArray()) bb.discardUntilDelimiterImplArrays(delimiter)
    else bb.discardUntilDelimiterImplDirect(delimiter)
}

private fun ByteBuffer.discardUntilDelimiterImplArrays(delimiter: Byte): Int {
    val array = array()!!
    val start = arrayOffset() + position()
    var i = start
    val end = i + remaining()
    if (end <= array.size) {
        while (i < end) {
            if (array[i] == delimiter) break
            i++
        }
    }

    position(i - arrayOffset())
    return i - start
}

private fun ByteBuffer.discardUntilDelimiterImplDirect(delimiter: Byte): Int {
    val start = position()
    var i = start

    while (i < limit()) {
        if (this[i] == delimiter) break
        i++
    }

    position(i)
    return i - start
}

internal actual fun Buffer.discardUntilDelimitersImpl(delimiter1: Byte, delimiter2: Byte): Int {
    val bb = readBuffer
    return if (bb.hasArray()) bb.discardUntilDelimitersImplArrays(delimiter1, delimiter2)
        else bb.discardUntilDelimitersImplDirect(delimiter1, delimiter2)
}

private fun ByteBuffer.discardUntilDelimitersImplArrays(delimiter1: Byte, delimiter2: Byte): Int {
    val array = array()!!
    val start = arrayOffset() + position()
    var i = start
    val end = i + remaining()
    if (end <= array.size) {
        while (i < end) {
            val v = array[i]
            if (v == delimiter1 || v == delimiter2) break
            i++
        }
    }

    position(i - arrayOffset())
    return i - start
}

private fun ByteBuffer.discardUntilDelimitersImplDirect(delimiter1: Byte, delimiter2: Byte): Int {
    val start = position()
    var i = start

    while (i < limit()) {
        val v = this[i]
        if (v == delimiter1 || v == delimiter2) break
        i++
    }

    position(i)
    return i - start
}

internal actual fun IoBuffer.readUntilDelimiterImpl(delimiter: Byte,
                                                    dst: ByteArray, offset: Int, length: Int): Int {
    assert(offset >= 0)
    assert(length >= 0)
    assert(offset + length <= dst.size)

    val bb = readBuffer
    return if (bb.hasArray()) bb.readUntilDelimiterArrays(delimiter, dst, offset, length)
    else bb.readUntilDelimiterDirect(delimiter, dst, offset, length)
}

private fun ByteBuffer.readUntilDelimiterDirect(delimiter: Byte,
                                                dst: ByteArray, offset: Int, length: Int): Int {
    return copyUntilDirect({ it == delimiter }, dst, offset, length)
}

private fun ByteBuffer.readUntilDelimiterArrays(delimiter: Byte,
                                                dst: ByteArray, offset: Int, length: Int): Int {
    return copyUntilArrays({ it == delimiter }, dst, offset, length)
}

internal actual fun Buffer.readUntilDelimitersImpl(
    delimiter1: Byte, delimiter2: Byte,
    dst: ByteArray, offset: Int, length: Int): Int {
    assert(offset >= 0)
    assert(length >= 0)
    assert(offset + length <= dst.size)
    assert(delimiter1 != delimiter2)

    val bb = readBuffer
    return if (bb.hasArray()) bb.readUntilDelimitersArrays(delimiter1, delimiter2, dst, offset, length)
    else bb.readUntilDelimitersDirect(delimiter1, delimiter2, dst, offset, length)
}

private fun ByteBuffer.readUntilDelimitersDirect(delimiter1: Byte, delimiter2: Byte,
                                                 dst: ByteArray, offset: Int, length: Int): Int {
    return copyUntilDirect({ it == delimiter1 || it == delimiter2 }, dst, offset, length)
}

private fun ByteBuffer.readUntilDelimitersArrays(delimiter1: Byte, delimiter2: Byte,
                                                 dst: ByteArray, offset: Int, length: Int): Int {
    return copyUntilArrays({ it == delimiter1 || it == delimiter2 }, dst, offset, length)
}

internal actual fun Buffer.readUntilDelimiterImpl(delimiter: Byte, dst: Output): Int {
    val bb = readBuffer
    return if (bb.hasArray()) bb.readUntilDelimiterArrays(delimiter, dst)
    else bb.readUntilDelimiterDirect(delimiter, dst)
}

internal fun ByteBuffer.readUntilDelimiterDirect(delimiter: Byte, dst: Output): Int {
    return copyUntilDirect({ it == delimiter }, dst)
}

internal fun ByteBuffer.readUntilDelimiterArrays(delimiter: Byte, dst: Output): Int {
    return copyUntilArrays({ it == delimiter }, dst)
}

internal actual fun Buffer.readUntilDelimitersImpl(delimiter1: Byte, delimiter2: Byte, dst: Output): Int {
    assert(delimiter1 != delimiter2)

    val bb = readBuffer
    return if (bb.hasArray()) bb.readUntilDelimitersArrays(delimiter1, delimiter2, dst)
    else bb.readUntilDelimitersDirect(delimiter1, delimiter2, dst)
}

internal fun ByteBuffer.readUntilDelimitersDirect(delimiter1: Byte, delimiter2: Byte,
                                                  dst: Output): Int {
    return copyUntilDirect({ it == delimiter1 || it == delimiter2 }, dst)
}

internal fun ByteBuffer.readUntilDelimitersArrays(delimiter1: Byte, delimiter2: Byte,
                                                  dst: Output): Int {
    return copyUntilArrays({ it == delimiter1 || it == delimiter2 }, dst)
}

private inline fun ByteBuffer.copyUntilDirect(predicate: (Byte) -> Boolean,
                                              dst: ByteArray, offset: Int, length: Int): Int {
    val start = position()
    var i = start
    val end = i + length
    while (i < limit() && i < end) {
        if (predicate(this[i])) break
        i++
    }

    val copied = i - start
    get(dst, offset, copied)
    return copied
}

private inline fun ByteBuffer.copyUntilArrays(predicate: (Byte) -> Boolean,
                                              dst: ByteArray, offset: Int, length: Int): Int {

    val array = array()!!
    val start = position() + arrayOffset()
    var i = start
    val end = i + minOf(length, remaining())
    if (end <= array.size) {
        while (i < end) {
            if (predicate(array[i])) break
            i++
        }
    }

    val copied = i - start
    System.arraycopy(array, start, dst, offset, copied)
    position(i - arrayOffset())
    return copied
}

private inline fun ByteBuffer.copyUntilDirect(predicate: (Byte) -> Boolean,
                                              dst: Output): Int {
    val bb = this
    var i = bb.position()
    var copiedTotal = 0

    dst.writeWhile { chunk ->
        val writeBuffer = chunk.writeBuffer
        val start = i
        val end = i + writeBuffer.remaining()

        while (i < bb.limit() && i < end) {
            if (predicate(bb[i])) break
            i++
        }

        val size = i - start
        val l = bb.limit()
        bb.position(start)
        bb.limit(i)
        writeBuffer.put(bb)
        bb.limit(l)
        chunk.afterWrite()

        copiedTotal += size
        !writeBuffer.hasRemaining() && i < bb.limit()
    }

    bb.position(i)
    return copiedTotal
}

private inline fun ByteBuffer.copyUntilArrays(predicate: (Byte) -> Boolean,
                                              dst: Output): Int {
    val bb = this
    val array = array()!!
    var i = bb.position() + arrayOffset()
    var copiedTotal = 0

    dst.writeWhile { chunk ->
        val writeBuffer = chunk.writeBuffer
        val start = i
        val end = minOf(i + writeBuffer.remaining(), limit() + arrayOffset())

        if (end <= array.size) {
            while (i < end) {
                if (predicate(array[i])) break
                i++
            }
        }

        val size = i - start
        val l = bb.limit()
        bb.position(start - bb.arrayOffset())
        bb.limit(bb.position() + size)
        writeBuffer.put(bb)
        bb.limit(l)
        chunk.afterWrite()

        copiedTotal += size
        !writeBuffer.hasRemaining() && bb.hasRemaining()
    }

    bb.position(i)
    return copiedTotal
}
