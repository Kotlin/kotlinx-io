package kotlinx.io.text

public expect abstract class CharsetDecoder

/**
 * Decoder's charset it is created for.
 */
public expect val CharsetDecoder.charset: Charset

public expect open class MalformedInputException(message: String) : Throwable
