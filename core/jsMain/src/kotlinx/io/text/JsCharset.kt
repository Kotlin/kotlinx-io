package kotlinx.io.text

actual abstract class Charset(internal val _name: String) {
    actual abstract fun newEncoder(): CharsetEncoder
    actual abstract fun newDecoder(): CharsetDecoder

    actual companion object {
        actual fun forName(name: String): Charset {
            if (name == "UTF-8" || name == "utf-8" || name == "UTF8" || name == "utf8") return Charsets.UTF_8
            if (name == "ISO-8859-1" || name == "iso-8859-1"
                || name.toLowerCase().replace('_', '-') == "iso-8859-1"
                || name == "latin1"
            ) {
                return Charsets.ISO_8859_1
            }
            throw IllegalArgumentException("Charset $name is not supported")
        }
    }
}

actual val Charset.name: String get() = _name

actual open class MalformedInputException actual constructor(message: String) : Throwable(message)

internal data class CharsetImpl(val name: String) : Charset(name) {
    override fun newEncoder(): CharsetEncoder = CharsetEncoderImpl(this)
    override fun newDecoder(): CharsetDecoder = CharsetDecoderImpl(this)
}
