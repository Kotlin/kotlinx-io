package kotlinx.io.text

@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")
actual typealias Charset = java.nio.charset.Charset

actual val Charset.name: String get() = name()

actual typealias CharsetEncoder = java.nio.charset.CharsetEncoder

actual val CharsetEncoder.charset: Charset get() = charset()

actual typealias CharsetDecoder = java.nio.charset.CharsetDecoder

actual val CharsetDecoder.charset: Charset get() = charset()

@Suppress("ACTUAL_WITHOUT_EXPECT")
public actual open class MalformedInputException actual constructor(message: String) : java.nio.charset.MalformedInputException(0) {
    private val _message = message

    override val message: String?
        get() = _message
}

public actual typealias Charsets = kotlin.text.Charsets
