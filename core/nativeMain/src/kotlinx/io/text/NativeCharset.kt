package kotlinx.io.text

import kotlinx.cinterop.*
import platform.iconv.*
import platform.posix.*

actual abstract class Charset(internal val _name: String) {
    actual abstract fun newEncoder(): CharsetEncoder
    actual abstract fun newDecoder(): CharsetDecoder

    actual companion object {
        actual fun forName(name: String): Charset {
            if (name == "UTF-8" || name == "utf-8" || name == "UTF8" || name == "utf8") return Charsets.UTF_8
            if (name == "ISO-8859-1" || name == "iso-8859-1") return Charsets.ISO_8859_1
            if (name == "UTF-16" || name == "utf-16" || name == "UTF16" || name == "utf16") return Charsets.UTF_16

            return CharsetImpl(name)
        }
    }
}

internal class CharsetImpl(name: String) : Charset(name) {
    init {
        val v = iconv_open(name, "UTF-8")
        checkErrors(v, name)
        iconv_close(v)
    }

    override fun newEncoder(): CharsetEncoder = CharsetEncoderImpl(this)
    override fun newDecoder(): CharsetDecoder = CharsetDecoderImpl(this)
}

actual val Charset.name: String get() = _name

private val negativePointer = (-1L).toCPointer<IntVar>()

private fun checkErrors(iconvOpenResults: COpaquePointer?, charset: String) {
    if (iconvOpenResults == null || iconvOpenResults === negativePointer) {
        throw IllegalArgumentException("Failed to open iconv for charset $charset with error code ${posix_errno()}")
    }
}

