package kotlinx.io.charsets

import kotlinx.io.core.*
import kotlinx.io.js.*

import kotlinx.cinterop.*
import kotlinx.io.pool.*
import platform.posix.memcpy
import platform.posix.memset

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

private class CharsetImpl(name: String) : Charset(name) {
    init {
        val v = iconv_open(name, "UTF-8")
        if (v == -1) throw IllegalArgumentException("Charset $name is not supported")
        iconv_close(v)
    }

    actual abstract fun newEncoder(): CharsetEncoder
    actual abstract fun newDecoder(): CharsetDecoder
}

actual val Charset.name: String get() = _name

// -----------------------

actual abstract class CharsetEncoder(internal val _charset: Charset)
private data class CharsetEncoderImpl(private val charset: Charset) : CharsetEncoder(charset)
actual val CharsetEncoder.charset: Charset get() = _charset

internal actual fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int, toIndex: Int, dst: BufferView): Int {
    TODO()
}

actual fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: BytePacketBuilder) {
    TODO()
}

internal actual fun CharsetEncoder.encodeComplete(dst: BufferView): Boolean = true

// ----------------------------------------------------------------------

actual abstract class CharsetDecoder(internal val _charset: Charset)
private data class CharsetDecoderImpl(private val charset: Charset) : CharsetDecoder(charset)
actual val CharsetDecoder.charset: Charset get() = _charset

actual fun CharsetDecoder.decode(input: ByteReadPacket, dst: Appendable) {
    TODO()
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
