package kotlinx.io.text

expect abstract class CharsetDecoder

/**
 * Decoder's charset it is created for.
 */
expect val CharsetDecoder.charset: Charset

public expect open class MalformedInputException(message: String) : Throwable
