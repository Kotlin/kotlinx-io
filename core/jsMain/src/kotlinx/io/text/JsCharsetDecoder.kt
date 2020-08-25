package kotlinx.io.text

public actual abstract class CharsetDecoder(internal val _charset: Charset)

internal data class CharsetDecoderImpl(private val charset: Charset) : CharsetDecoder(charset)

public actual val CharsetDecoder.charset: Charset get() = _charset

