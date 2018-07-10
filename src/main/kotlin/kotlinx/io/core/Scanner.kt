package kotlinx.io.core

/**
 * Discards bytes until [delimiter] occurred
 * @return number of bytes discarded
 */
fun Input.discardUntilDelimiter(delimiter: Byte): Long {
    var discardedTotal = 0L

    takeWhile { chunk ->
        val discarded = chunk.discardUntilDelimiterImpl(delimiter)
        discardedTotal += discarded
        discarded > 0 && !chunk.canRead()
    }

    return discardedTotal
}

/**
 * Discards bytes until of of the specified delimiters [delimiter1] or [delimiter2] occurred
 * @return number of bytes discarded
 */
fun Input.discardUntilDelimiters(delimiter1: Byte, delimiter2: Byte): Long {
    var discardedTotal = 0L

    takeWhile { chunk ->
        val discarded = chunk.discardUntilDelimitersImpl(delimiter1, delimiter2)
        discardedTotal += discarded
        discarded > 0 && !chunk.canRead()
    }

    return discardedTotal
}

/**
 * Copies to [dst] array at [offset] at most [length] bytes or until the specified [delimiter] occurred.
 * @return number of bytes copied
 */
fun Input.readUntilDelimiter(delimiter: Byte, dst: ByteArray, offset: Int = 0, length: Int = dst.size): Int {
    var currentOffset = offset
    var dstRemaining = length

    takeWhile { chunk ->
        val copied = chunk.readUntilDelimiter(delimiter, dst, currentOffset, dstRemaining)
        currentOffset += copied
        dstRemaining -= copied
        dstRemaining > 0 && !chunk.canRead()
    }

    return currentOffset - offset
}

/**
 * Copies to [dst] array at [offset] at most [length] bytes or until one of the specified delimiters
 * [delimiter1] or [delimiter2] occurred.
 * @return number of bytes copied
 */
fun Input.readUntilDelimiters(delimiter1: Byte, delimiter2: Byte,
                              dst: ByteArray, offset: Int = 0, length: Int = dst.size): Int {
    if (delimiter1 == delimiter2) return readUntilDelimiter(delimiter1, dst, offset, length)

    var currentOffset = offset
    var dstRemaining = length

    takeWhile {  chunk ->
        val copied = chunk.readUntilDelimiters(delimiter1, delimiter2, dst, currentOffset, dstRemaining)
        currentOffset += copied
        dstRemaining -= copied
        !chunk.canRead() && dstRemaining > 0
    }

    return currentOffset - offset
}

/**
 * Copies to [dst] output until the specified [delimiter] occurred.
 * @return number of bytes copied
 */
fun Input.readUntilDelimiter(delimiter: Byte, dst: Output): Long {
    var copiedTotal = 0L
    takeWhile {  chunk ->
        val copied = chunk.readUntilDelimiter(delimiter, dst)
        copiedTotal += copied
        !chunk.canRead()
    }

    return copiedTotal
}

/**
 * Copies to [dst] output until one of the specified delimiters
 * [delimiter1] or [delimiter2] occurred.
 * @return number of bytes copied
 */
fun Input.readUntilDelimiters(delimiter1: Byte, delimiter2: Byte, dst: Output): Long {
    var copiedTotal = 0L

    takeWhile {  chunk ->
        val copied = chunk.readUntilDelimiters(delimiter1, delimiter2, dst)
        copiedTotal += copied
        !chunk.canRead()
    }

    return copiedTotal
}

internal expect fun IoBuffer.discardUntilDelimiterImpl(delimiter: Byte): Int

internal expect fun IoBuffer.discardUntilDelimitersImpl(delimiter1: Byte, delimiter2: Byte): Int


internal expect fun IoBuffer.readUntilDelimiter(delimiter: Byte,
                                                dst: ByteArray, offset: Int, length: Int): Int

internal expect fun IoBuffer.readUntilDelimiters(delimiter1: Byte, delimiter2: Byte,
                                                 dst: ByteArray, offset: Int, length: Int): Int

internal expect fun IoBuffer.readUntilDelimiter(delimiter: Byte,
                                                dst: Output): Int

internal expect fun IoBuffer.readUntilDelimiters(delimiter1: Byte, delimiter2: Byte,
                                                 dst: Output): Int

