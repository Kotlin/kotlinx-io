package kotlinx.io.text

import kotlinx.io.*

actual object Charsets {
    actual val UTF_8: Charset = CharsetImpl("UTF-8")
    actual val ISO_8859_1: Charset = CharsetImpl("ISO-8859-1")
    internal val UTF_16: Charset = CharsetImpl(platformUtf16)
}

private val platformUtf16: String by lazy { if (ByteOrder.native == ByteOrder.BIG_ENDIAN) "UTF-16BE" else "UTF-16LE" }

actual open class MalformedInputException actual constructor(message: String) : Throwable(message)
