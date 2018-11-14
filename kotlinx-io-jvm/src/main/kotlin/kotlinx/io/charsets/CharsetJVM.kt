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

actual fun CharsetEncoder.encodeToByteArray(input: CharSequence, fromIndex: Int, toIndex: Int): ByteArray {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    if (input is String) {
        if (fromIndex == 0 && toIndex == input.length)
            return (input as java.lang.String).getBytes(charset())
        return (input.substring(fromIndex, toIndex) as java.lang.String).getBytes(charset())
    }

    return encodeToByteArraySlow(input, fromIndex, toIndex)
}

private fun CharsetEncoder.encodeToByteArraySlow(input: CharSequence, fromIndex: Int, toIndex: Int): ByteArray {
    val result = encode(CharBuffer.wrap(input, fromIndex, toIndex))

    val existingArray = when {
        result.hasArray() && result.arrayOffset() == 0 -> result.array()?.takeIf { it.size == result.remaining() }
        else -> null
    }

    return existingArray ?: ByteArray(result.remaining()).also { result.get(it) }
}

internal actual fun CharsetEncoder.encodeImpl(input: CharSequence, fromIndex: Int, toIndex: Int, dst: IoBuffer): Int {
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

    val tmp = IoBuffer.Pool.borrow()
    var readSize = 1

    try {
        tmp.writeDirect(0) { tmpBb ->
            val cb = tmpBb.asCharBuffer()!!

            while (input.remaining > 0) {
                cb.clear()

                val chunk = input.prepareReadHead(readSize)
                if (chunk == null) {
                    if (readSize != 1) throw MalformedInputException("...")
                    break
                }

                val rc = chunk.decodeUTF8 { ch ->
                    if (cb.hasRemaining()) {
                        cb.put(ch)
                        true
                    } else false
                }

                input.updateHeadRemaining(chunk.readRemaining)

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
        tmp.release(IoBuffer.Pool)
    }
}

internal actual fun CharsetEncoder.encodeComplete(dst: IoBuffer): Boolean {
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

    var readSize = 1

    input.takeWhileSize { buffer: IoBuffer ->
        val rem = max - copied
        if (rem == 0) return@takeWhileSize 0


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

actual fun CharsetDecoder.decodeExactBytes(input: Input, inputLength: Int): String {
    if (inputLength == 0) return ""
    if (input is ByteReadPacketBase && input.headRemaining >= inputLength) {
        // if we have a packet or a buffered input with the first head containing enough bytes
        // then we can try fast-path
        if (input.head.readBuffer.hasArray()) {
            // the most performant way is to use String ctor of ByteArray
            // on JVM9+ with string compression enabled it will do System.arraycopy and lazy decoding that is blazing fast
            // on older JVMs it is still the fastest way
            val bb = input.head.readBuffer
            val text = String(bb.array(), bb.arrayOffset() + bb.position(), inputLength, charset())
            input.discardExact(inputLength)
            return text
        }

        // the second fast-path is slower however it is still faster than general way
        return decodeImplByteBuffer(input, inputLength)
    }

    return decodeImplSlow(input, inputLength)
}

private fun CharsetDecoder.decodeImplByteBuffer(input: ByteReadPacketBase, inputLength: Int): String {
    val cb = CharBuffer.allocate(inputLength)
    val bb = input.head.readBuffer
    val limit = bb.limit()
    bb.limit(bb.position() + inputLength)

    val rc = decode(input.head.readBuffer, cb, true)
    if (rc.isMalformed || rc.isUnmappable) rc.throwExceptionWrapped()

    bb.limit(limit)
    cb.flip()
    return cb.toString()
}

private fun CharsetDecoder.decodeImplSlow(input: Input, inputLength: Int): String {
    val cb = CharBuffer.allocate(inputLength)
    var remainingInputBytes = inputLength
    var lastChunk = false

    var readSize = 1

    input.takeWhileSize { buffer: IoBuffer ->
        if (!cb.hasRemaining() || remainingInputBytes == 0) return@takeWhileSize 0

        buffer.readDirect { bb: ByteBuffer ->
            val limitBefore = bb.limit()
            val positionBefore = bb.position()

            lastChunk = limitBefore - positionBefore >= remainingInputBytes

            if (lastChunk) {
                bb.limit(positionBefore + remainingInputBytes)
            }
            val rc = decode(bb, cb, lastChunk)

            if (rc.isMalformed || rc.isUnmappable) rc.throwExceptionWrapped()
            if (rc.isUnderflow && bb.hasRemaining()) {
                readSize++
            } else {
                readSize = 1
            }

            bb.limit(limitBefore)
            remainingInputBytes -= bb.position() - positionBefore
        }
        readSize
    }

    if (cb.hasRemaining() && !lastChunk) {
        val rc = decode(EmptyByteBuffer, cb, true)

        if (rc.isMalformed || rc.isUnmappable) rc.throwExceptionWrapped()
    }

    if (remainingInputBytes > 0) {
        throw EOFException("Not enough bytes available: had only ${inputLength - remainingInputBytes} instead of $inputLength")
    }
    if (remainingInputBytes < 0) {
        throw AssertionError("remainingInputBytes < 0")
    }

    cb.flip()
    return cb.toString()
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
