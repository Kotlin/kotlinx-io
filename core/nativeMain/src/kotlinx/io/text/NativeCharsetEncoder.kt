package kotlinx.io.text

actual abstract class CharsetEncoder(internal val _charset: Charset)
internal data class CharsetEncoderImpl(private val charset: Charset) : CharsetEncoder(charset)

actual val CharsetEncoder.charset: Charset get() = _charset

