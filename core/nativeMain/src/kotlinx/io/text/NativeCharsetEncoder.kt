package kotlinx.io.text

public actual abstract class CharsetEncoder(internal val _charset: Charset)
internal data class CharsetEncoderImpl(private val charset: Charset) : CharsetEncoder(charset)

public actual val CharsetEncoder.charset: Charset get() = _charset

