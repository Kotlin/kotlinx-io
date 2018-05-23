package kotlinx.io.charsets

import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import java.nio.*
import java.nio.charset.*

private const val DECODE_CHAR_BUFFER_SIZE = 8192

@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")
actual typealias Charset = java.nio.charset.Charset

actual val Charset.name: String get() = name()

actual typealias CharsetEncoder = java.nio.charset.CharsetEncoder

actual val CharsetEncoder.charset: Charset get() = charset()

internal actual fun CharsetEncoder.encodeImpl(input: CharSequence, fromIndex: Int, toIndex: Int, dst: BufferView): Int {
    val cb = CharBuffer.wrap(input, fromIndex, toIndex)
    val before = cb.remaining()

    dst.writeDirect(0) { bb ->
        val result = encode(cb, bb, false)
        if (result.isMalformed || result.isUnmappable) result.throwExceptionWrapped()
    }

    return before - cb.remaining()
}

actual fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: Output) {
    if (charset === Charsets.UTF_8) {
        dst.writePacket(input)
        return
    }

    val tmp = BufferView.Pool.borrow()
    var readSize = 1

    try {
        tmp.writeDirect(0) { tmpBb ->
            val cb = tmpBb.asCharBuffer()!!

            while (input.remaining > 0) {
                cb.clear()

                @Suppress("DEPRECATION_ERROR")
                val chunk = input.`$prepareRead$`(readSize)
                if (chunk == null) {
                    if (readSize != 1) throw MalformedInputException("...")
                    break
                }

                @Suppress("INVISIBLE_MEMBER")
                val rc = chunk.decodeUTF8 { ch ->
                    if (cb.hasRemaining()) {
                        cb.put(ch)
                        true
                    } else false
                }

                @Suppress("DEPRECATION_ERROR")
                input.`$updateRemaining$`(chunk.readRemaining)

                cb.flip()

                var writeSize = 1
                if (cb.hasRemaining()) {
                    dst.writeWhileSize { view ->
                        view.writeDirect(writeSize) { to ->
                            val cr = encode(cb, to, false)
                            if (cr.isUnmappable || cr.isMalformed) cr.throwExceptionWrapped()
                            if (cr.isOverflow && to.hasRemaining()) writeSize++
                            else writeSize = 1
                        }
                        if (cb.hasRemaining()) writeSize else 0
                    }
                }

                if (rc > 0) {
                    readSize = rc
                    break
                }
            }

            cb.clear()
            cb.flip()

            var completeSize = 1
            dst.writeWhileSize { chunk ->
                chunk.writeDirect(completeSize) { to ->
                    val cr = encode(cb, to, true)
                    if (cr.isMalformed || cr.isUnmappable) cr.throwExceptionWrapped()
                    if (cr.isOverflow) completeSize++
                    else completeSize = 0
                }

                completeSize
            }
        }
    } finally {
        tmp.release(BufferView.Pool)
    }
}

internal actual fun CharsetEncoder.encodeComplete(dst: BufferView): Boolean {
    var completed = false

    dst.writeDirect(0) { bb ->
        val result = encode(EmptyCharBuffer, bb, true)
        if (result.isMalformed || result.isUnmappable) result.throwExceptionWrapped()
        if (result.isUnderflow) {
            completed = true
        }
    }

    return completed
}

// -----------------------

actual typealias CharsetDecoder = java.nio.charset.CharsetDecoder

actual val CharsetDecoder.charset: Charset get() = charset()!!

actual fun CharsetDecoder.decode(input: Input, dst: Appendable, max: Int): Int {
    var copied = 0
    val cb = CharBuffer.allocate(DECODE_CHAR_BUFFER_SIZE)

    input.takeWhileSize { buffer: BufferView ->
        val rem = max - copied
        if (rem == 0) return@takeWhileSize 0

        var readSize = 1

        buffer.readDirect { bb: ByteBuffer ->
            cb.clear()
            if (rem < DECODE_CHAR_BUFFER_SIZE) {
                cb.limit(rem)
            }
            val rc = decode(bb, cb, false)
            cb.flip()
            copied += cb.remaining()
            dst.append(cb)

            if (rc.isMalformed || rc.isUnmappable) rc.throwExceptionWrapped()
            if (rc.isUnderflow && bb.hasRemaining()) {
                readSize++
            } else {
                readSize = 1
            }
        }
        readSize
    }

    while (true) {
        cb.clear()
        val rem = max - copied
        if (rem == 0) break
        if (rem < DECODE_CHAR_BUFFER_SIZE) {
            cb.limit(rem)
        }
        val cr = decode(EmptyByteBuffer, cb, true)
        cb.flip()
        copied += cb.remaining()
        dst.append(cb)

        if (cr.isUnmappable || cr.isMalformed) cr.throwExceptionWrapped()
        if (cr.isOverflow) continue
        break
    }

    return copied
}

private fun CoderResult.throwExceptionWrapped() {
    try {
        throwException()
    } catch (original: java.nio.charset.MalformedInputException) {
        throw MalformedInputException(original.message ?: "Failed to decode bytes")
    }
}

// ----------------------------------

actual typealias Charsets = kotlin.text.Charsets

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class MalformedInputException actual constructor(message: String) : java.nio.charset.MalformedInputException(0) {
    private val _message = message

    override val message: String?
        get() = _message
}

private val EmptyCharBuffer = CharBuffer.allocate(0)!!
private val EmptyByteBuffer = ByteBuffer.allocate(0)!!