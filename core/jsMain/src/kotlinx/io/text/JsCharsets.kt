package kotlinx.io.text

actual object Charsets {
    actual val UTF_8: Charset = CharsetImpl("UTF-8")
    actual val ISO_8859_1: Charset = CharsetImpl("ISO-8859-1")
}
