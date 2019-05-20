package kotlinx.io.text

import kotlinx.io.*

expect abstract class CharsetDecoder

/**
 * Decoder's charset it is created for.
 */
expect val CharsetDecoder.charset: Charset

expect class MalformedInputException(message: String) : Throwable
