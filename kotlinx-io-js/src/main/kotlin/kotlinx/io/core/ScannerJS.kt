package kotlinx.io.core

import org.khronos.webgl.*

internal actual fun IoBuffer.discardUntilDelimiterImpl(delimiter: Byte): Int {
    val content = Int8Array(content)
    var idx = readPosition
    val end = writePosition

    while (idx < end) {
        if (content[idx] == delimiter) break
        idx++
    }

    val start = readPosition
    readPosition = idx
    return idx - start
}

internal actual fun IoBuffer.discardUntilDelimitersImpl(delimiter1: Byte, delimiter2: Byte): Int {
    val content = Int8Array(content)
    var idx = readPosition
    val end = writePosition

    while (idx < end) {
        val v = content[idx]
        if (v == delimiter1 || v == delimiter2) break
        idx++
    }

    val start = readPosition
    readPosition = idx
    return idx - start
}


internal actual fun IoBuffer.readUntilDelimiterImpl(delimiter: Byte,
                                                    dst: ByteArray, offset: Int, length: Int): Int {
    check(offset >= 0)
    check(length >= 0)
    check(offset + length <= dst.size)

    return readUntilImpl({ it == delimiter }, dst, offset, length)
}

internal actual fun IoBuffer.readUntilDelimitersImpl(delimiter1: Byte, delimiter2: Byte,
                                                     dst: ByteArray, offset: Int, length: Int): Int {
    check(offset >= 0)
    check(length >= 0)
    check(offset + length <= dst.size)
    check(delimiter1 != delimiter2)

    return readUntilImpl({ it == delimiter1 || it == delimiter2 }, dst, offset, length)
}

internal actual fun IoBuffer.readUntilDelimiterImpl(delimiter: Byte, dst: Output): Int {
    return readUntilImpl({ it == delimiter }, dst)
}

internal actual fun IoBuffer.readUntilDelimitersImpl(delimiter1: Byte, delimiter2: Byte, dst: Output): Int {
    check(delimiter1 != delimiter2)

    return readUntilImpl({ it == delimiter1 || it == delimiter2 }, dst)
}

private inline fun IoBuffer.readUntilImpl(predicate: (Byte) -> Boolean,
                                          dst: ByteArray, offset: Int, length: Int): Int {

    val array = Int8Array(content)
    val start = readPosition
    var i = start
    val end = i + minOf(length, readRemaining)

    while (i < end) {
        if (predicate(array[i])) break
        i++
    }

    val copied = i - start
    val dstArray = dst.asDynamic() as Int8Array
    dstArray.set(array.subarray(start, end), offset)
    readPosition = i

    return copied
}

private inline fun IoBuffer.readUntilImpl(predicate: (Byte) -> Boolean,
                                          dst: Output): Int {
    val array = Int8Array(content)
    var i = readPosition
    var copiedTotal = 0

    dst.writeWhile { chunk ->
        chunk.writeFully(chunk, 0)
        val start = i
        val end = minOf(i + chunk.writeRemaining, writePosition)

        while (i < end) {
            if (predicate(array[i])) break
            i++
        }

        val size = i - start

        Int8Array(chunk.content).set(array.subarray(start, i), chunk.writePosition)
        chunk.writePosition += size
        copiedTotal += size

        chunk.writeRemaining == 0 && i < end
    }

    readPosition = i
    return copiedTotal
}