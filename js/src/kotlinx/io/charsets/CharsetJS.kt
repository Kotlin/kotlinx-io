package kotlinx.io.charsets

import kotlinx.io.core.*
import kotlinx.io.js.*
import org.khronos.webgl.*

actual abstract class Charset(internal val _name: String) {
    actual abstract fun newEncoder(): CharsetEncoder
    actual abstract fun newDecoder(): CharsetDecoder

    actual companion object {
        actual fun forName(name: String): Charset {
            if (name == "UTF-8" || name == "utf-8" || name == "UTF8" || name == "utf8") return Charsets.UTF_8
            throw IllegalArgumentException("Charset $name is not supported")
        }

    }
}

actual val Charset.name: String get() = _name

// -----------------------

actual abstract class CharsetEncoder(internal val _charset: Charset)
private data class CharsetEncoderImpl(private val charset: Charset) : CharsetEncoder(charset)
actual val CharsetEncoder.charset: Charset get() = _charset

actual fun CharsetEncoder.encodeToByteArray(input: CharSequence, fromIndex: Int, toIndex: Int): ByteArray
        = encodeToByteArrayImpl1(input, fromIndex, toIndex)

internal actual fun CharsetEncoder.encodeImpl(input: CharSequence, fromIndex: Int, toIndex: Int, dst: IoBuffer): Int {
    require(fromIndex <= toIndex)
    require(charset === Charsets.UTF_8) { "Only UTF-8 encoding is supported in JS" }

    val encoder = TextEncoderCtor()  // Only UTF-8 is supported so we know that at most 6 bytes per character is used
    var start = fromIndex
    var dstRemaining = dst.writeRemaining

    while (start < toIndex && dstRemaining > 0) {
        val numChars = minOf(toIndex - start, dstRemaining / 6).coerceAtLeast(1)
        val dropLastChar = input[start + numChars - 1].isHighSurrogate()
        val endIndexExclusive = when {
            dropLastChar && numChars == 1 -> start + 2
            dropLastChar -> start + numChars - 1
            else -> start + numChars
        }

        val array1 = encoder.encode(input.substring(start, endIndexExclusive))
        if (array1.length > dst.writeRemaining) break
        dst.writeFully(array1, 0, array1.length)
        start = endIndexExclusive
        dstRemaining -= array1.length
    }

    return start - fromIndex
}

actual fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: Output) {
    require(charset === Charsets.UTF_8)
    // we only support UTF-8 so as far as input is UTF-8 encoded string then we simply copy bytes
    dst.writePacket(input)
}

internal actual fun CharsetEncoder.encodeComplete(dst: IoBuffer): Boolean = true

// ----------------------------------------------------------------------

actual abstract class CharsetDecoder(internal val _charset: Charset)

private data class CharsetDecoderImpl(private val charset: Charset) : CharsetDecoder(charset)

actual val CharsetDecoder.charset: Charset get() = _charset

actual fun CharsetDecoder.decode(input: Input, dst: Appendable, max: Int): Int {
    val decoder = TextDecoderFatal(charset.name, true)
    var copied = 0

    input.takeWhileSize { buffer ->
        val rem = max - copied
        if (rem == 0) return@takeWhileSize 0

        copied += buffer.readText(decoder, dst, buffer.next == null, rem)
        1
    }

    if (copied < max) {
        val s = decodeWrap { decoder.decode() }
        if (s.length > max - copied) {
            throw UnsupportedOperationException("Partial trailing characters are not supported")
        }

        dst.append(s)
        copied += s.length
    }

    return copied
}

actual fun CharsetDecoder.decodeExactBytes(input: Input, inputLength: Int): String {
    if (inputLength == 0) return ""
    if (input is ByteReadPacketBase && input.headRemaining >= inputLength) {
        val decoder = TextDecoderFatal(charset._name, true)

        val head = input.head
        val text = decodeWrap {
            when {
                head.readPosition == 0 && inputLength == head.content.byteLength -> decoder.decode(head.content)
                else -> decoder.decode(Int8Array(head.content, head.readPosition, inputLength))
            }
        }

        input.discardExact(inputLength)
        return text
    }

    return decodeExactBytesSlow(input, inputLength)
}

// -----------------------------------------------------------

actual object Charsets {
    actual val UTF_8: Charset = CharsetImpl("UTF-8")
}

private data class CharsetImpl(val name: String) : Charset(name) {
    override fun newEncoder(): CharsetEncoder = CharsetEncoderImpl(this)
    override fun newDecoder(): CharsetDecoder = CharsetDecoderImpl(this)
}


actual class MalformedInputException actual constructor(message: String) : Throwable(message)


private fun CharsetDecoder.decodeExactBytesSlow(input: Input, inputLength: Int): String {
    val decoder = TextDecoderFatal(charset.name, true)
    var inputRemaining = inputLength
    val sb = StringBuilder(inputLength)

    decodeWrap {
        input.takeWhileSize(6) { buffer ->
            val chunkSize = buffer.readRemaining
            val size = minOf(chunkSize, inputRemaining)
            val text = when {
                buffer.readPosition == 0 && buffer.content.byteLength == size -> decoder.decodeStream(buffer.content, true)
                else -> decoder.decodeStream(Int8Array(buffer.content, buffer.readPosition, size), true)
            }
            sb.append(text)

            buffer.discardExact(size)
            inputRemaining -= size

            if (inputRemaining > 0) 6 else 0
        }

        if (inputRemaining > 0) {
            input.takeWhile { buffer ->
                val chunkSize = buffer.readRemaining
                val size = minOf(chunkSize, inputRemaining)
                val text = when {
                    buffer.readPosition == 0 && buffer.content.byteLength == size -> decoder.decode(buffer.content)
                    else -> decoder.decodeStream(Int8Array(buffer.content, buffer.readPosition, size), true)
                }
                sb.append(text)
                buffer.discardExact(size)
                inputRemaining -= size
                true
            }
        }

        sb.append(decoder.decode())
    }

    return sb.toString()
}
