package kotlinx.io.text

@Suppress("NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS")
public actual typealias Charset = java.nio.charset.Charset

public actual val Charset.name: String get() = name()

public actual typealias CharsetEncoder = java.nio.charset.CharsetEncoder

public actual val CharsetEncoder.charset: Charset get() = charset()

public actual typealias CharsetDecoder = java.nio.charset.CharsetDecoder

public actual val CharsetDecoder.charset: Charset get() = charset()

@Suppress("ACTUAL_WITHOUT_EXPECT")
public actual open class MalformedInputException actual constructor(message: String) : java.nio.charset.MalformedInputException(0) {
    private val _message = message

    override val message: String?
        get() = _message
}

public actual typealias Charsets = kotlin.text.Charsets
