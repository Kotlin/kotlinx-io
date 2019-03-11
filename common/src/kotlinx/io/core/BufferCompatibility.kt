@file:Suppress("DeprecatedCallableAddReplaceWith")

package kotlinx.io.core

import kotlinx.io.bits.*
import kotlinx.io.charsets.*
import kotlinx.io.core.internal.*
import kotlinx.io.core.internal.require
import kotlinx.io.errors.*
import kotlinx.io.pool.*


@Deprecated("Use discard with Int parameter. No replacement")
fun Buffer.discard(n: Long): Long = minOf(readRemaining.toLong(), n).toInt().also { discard(it) }.toLong()

fun Buffer.discardExact(n: Int) {
    discard(n)
}

/**
 * Copy available bytes to the specified [buffer] but keep them available.
 * If the underlying implementation could trigger
 * bytes population from the underlying source and block until any bytes available
 *
 * Very similar to [readAvailable] but don't discard copied bytes.
 *
 * @return number of bytes were copied
 */
fun Buffer.peekTo(buffer: Buffer): Int {
    val size = minOf(readRemaining, buffer.writeRemaining)
    memory.copyTo(buffer.memory, readPosition, size, buffer.writePosition)
    discard(size)
    buffer.commitWritten(size)
    return size
}

/**
 * Write byte [value] repeated the specified [times].
 */
fun Buffer.fill(times: Int, value: Byte) {
    require(times >= 0) { "times shouldn't be negative: $times" }
    require(times <= writeRemaining) { "times shouldn't be greater than the write remaining space: $times > $writeRemaining" }

    memory.fill(writePosition, times, value)
    commitWritten(times)
}

/**
 * Write unsigned byte [value] repeated the specified [times].
 */
fun Buffer.fill(times: Int, value: UByte) {
    fill(times, value.toByte())
}

/**
 * Write byte [v] value repeated [n] times.
 */
@Deprecated("Use fill with n with type Int")
fun Buffer.fill(n: Long, v: Byte) {
    fill(n.toIntOrFail("n"), v)
}

/**
 * Push back [n] bytes: only possible if there were at least [n] bytes read before this operation.
 */
@Deprecated("Use rewind instead", ReplaceWith("rewind(n)"))
fun Buffer.pushBack(n: Int): Unit = rewind(n)

@Deprecated("Use duplicate instead", ReplaceWith("duplicate()"))
fun Buffer.makeView(): Buffer = duplicate()

@Deprecated("Use duplicate instead", ReplaceWith("duplicate()"))
fun ChunkBuffer.makeView(): ChunkBuffer = duplicate()

@Deprecated("Does nothing.")
fun Buffer.flush() {
}

internal fun Buffer.appendChars(csq: CharArray, start: Int, end: Int): Int {
    return appendChars(CharArraySequence(csq, 0, csq.size), start, end)
}

internal fun Buffer.appendChars(csq: CharSequence, start: Int, end: Int): Int {
    var charactersWritten: Int

    write { dst, dstStart, dstEndExclusive ->
        val result = dst.encodeUTF8(csq, start, end, dstStart, dstEndExclusive)
        charactersWritten = result.characters.toInt()
        result.bytes.toInt()
    }

    return start + charactersWritten
}

@Deprecated("This is no longer supported. Use a packet builder to append characters instead.")
fun Buffer.append(c: Char): Buffer {
    write { memory, start, endExclusive ->
        val size = memory.putUtf8Char(start, c.toInt())
        when {
            size > endExclusive - start -> appendFailed(1)
            else -> size
        }
    }

    return this
}

@Deprecated("This is no longer supported. Use a packet builder to append characters instead.")
fun Buffer.append(csq: CharSequence?): Buffer {
    if (csq == null) {
        return append("null")
    }

    return append(csq, 0, csq.length)
}

@Deprecated("This is no longer supported. Use a packet builder to append characters instead.")
fun Buffer.append(csq: CharSequence?, start: Int, end: Int): Buffer = apply {
    if (csq == null) {
        return append("null", start, end)
    }

    if (appendChars(csq, start, end) != end) {
        appendFailed(end - start)
    }
}

private fun appendFailed(length: Int): Nothing {
    throw BufferLimitExceededException("Not enough free space available to write $length character(s).")
}

@Deprecated("This is no longer supported. Use a packet builder to append characters instead.")
fun Buffer.append(csq: CharArray, start: Int, end: Int): Buffer {
    return append(CharArraySequence(csq, 0, csq.size), start, end)
}

@Deprecated("This is no longer supported. Read from a packet instead.")
fun Buffer.readText(decoder: CharsetDecoder, out: Appendable, lastBuffer: Boolean, max: Int = Int.MAX_VALUE): Int {
    return decoder.decodeBuffer(this, out, lastBuffer, max)
}

/**
 * releases buffer view and returns it to the [pool] if there are no more usages. Based on simple ref-counting so
 * it is very fragile.
 */
@Suppress("DEPRECATION")
fun IoBuffer.release(pool: ObjectPool<IoBuffer>) {
    // TODO ???
    @Suppress("UNCHECKED_CAST")
    (this as ChunkBuffer).release(pool as ObjectPool<ChunkBuffer>)
}

/**
 * Peek the next unsigned byte or return `-1` if no more bytes available for reading. No bytes will be marked
 * as consumed in any case.
 * @see [Buffer.tryPeekByte]
 */
@Deprecated("Use tryPeekByte instead", replaceWith = ReplaceWith("tryPeekByte()"))
fun Buffer.tryPeek(): Int = tryPeekByte()

fun Buffer.readFully(dst: Array<Byte>, offset: Int = 0, length: Int = dst.size - offset) {
    read { memory, start, endExclusive ->
        if (endExclusive - start < length) {
            throw EOFException("Not enough bytes available to read $length bytes")
        }

        for (index in 0 until length) {
            dst[index + offset] = memory[index + start]
        }

        length
    }
}

@Deprecated(
    "This is no longer supported. All operations are big endian by default. Use readXXXLittleEndian " +
        "to read primitives in little endian",
    level = DeprecationLevel.ERROR
)
var Buffer.byteOrder: ByteOrder
    get() = ByteOrder.BIG_ENDIAN
    set(newOrder) {
        if (newOrder != ByteOrder.BIG_ENDIAN) throw UnsupportedOperationException("Only BIG_ENDIAN is supported")
    }
