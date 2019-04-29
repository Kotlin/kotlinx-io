package kotlinx.io.text

actual abstract class CharsetDecoder(internal val _charset: Charset)
internal data class CharsetDecoderImpl(private val charset: Charset) : CharsetDecoder(charset)

actual val CharsetDecoder.charset: Charset get() = _charset
