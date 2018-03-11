package kotlinx.io.charsets

import kotlinx.io.core.*
import java.nio.*

@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")
actual typealias Charset = java.nio.charset.Charset
actual val Charset.name: String get() = name()

actual typealias CharsetEncoder = java.nio.charset.CharsetEncoder
actual val CharsetEncoder.charset: Charset get() = charset()

internal actual fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int, toIndex: Int, dst: BufferView): Int {
    val cb = CharBuffer.wrap(input, fromIndex, toIndex)
    val before = cb.remaining()

    dst.writeDirect(0) { bb ->
        val result = encode(cb, bb, false)
        if (result.isMalformed || result.isUnmappable) result.throwException()
    }

    return before - cb.remaining()
}

actual fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: BytePacketBuilder) {
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
                while (cb.hasRemaining()) {
                    dst.writeDirect(writeSize) { to ->
                        val cr = encode(cb, to, false)
                        if (cr.isUnmappable || cr.isMalformed) cr.throwException()
                        if (cr.isOverflow && to.hasRemaining()) writeSize ++
                        else writeSize = 1
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
            while (completeSize > 0) {
                dst.writeDirect(completeSize) { to ->
                    val cr = encode(cb, to, true)
                    if (cr.isMalformed || cr.isUnmappable) cr.throwException()
                    if (cr.isOverflow) completeSize++
                    else completeSize = 0
                }
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
        if (result.isMalformed || result.isUnmappable) result.throwException()
        if (result.isUnderflow) {
            completed = true
        }
    }

    return completed
}

// -----------------------

actual typealias CharsetDecoder = java.nio.charset.CharsetDecoder
actual val CharsetDecoder.charset: Charset get() = charset()!!

actual fun CharsetDecoder.decode(input: ByteReadPacket, dst: Appendable)  {
    var readSize = 1

    val cb = CharBuffer.allocate(8192)
    while (true) {
        @Suppress("DEPRECATION_ERROR")
        val buffer: BufferView? = input.`$prepareRead$`(readSize)

        if (buffer == null) {
            if (readSize != 1) throw MalformedInputException("Not enough bytes available to decode a character: should be at least $readSize")
            break
        }

        try {
            buffer.readDirect { bb: ByteBuffer ->
                cb.clear()
                val rc = decode(bb, cb, false)
                cb.flip()
                dst.append(cb)

                if (rc.isMalformed || rc.isUnmappable) rc.throwException()
                if (rc.isUnderflow && bb.hasRemaining()) {
                    readSize++
                } else {
                    readSize = 1
                }
            }
        } finally {
            @Suppress("DEPRECATION_ERROR")
            input.`$updateRemaining$`(buffer.readRemaining)
        }
    }

    while (true) {
        cb.clear()
        val cr = decode(EmptyByteBuffer, cb, true)
        cb.flip()
        dst.append(cb)

        if (cr.isUnmappable || cr.isMalformed) cr.throwException()
        if (cr.isOverflow) continue
        break
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