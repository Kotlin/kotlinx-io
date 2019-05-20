package kotlinx.io.text

expect abstract class CharsetEncoder

expect val CharsetEncoder.charset: Charset

/*
expect fun CharsetEncoder.encodeToByteArray(
    input: CharSequence,
    fromIndex: Int = 0,
    toIndex: Int = input.length
): ByteArray

@Suppress("NOTHING_TO_INLINE")
inline fun String.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray =
    charset.newEncoder().encodeToByteArray(this, 0, length)

*/
/**
 * Create an instance of [String] from the specified [bytes] range starting at [offset] and bytes [length]
 * interpreting characters in the specified [charset].
 *//*

@Suppress("FunctionName")
expect fun String(
    bytes: ByteArray,
    offset: Int = 0,
    length: Int = bytes.size,
    charset: Charset = Charsets.UTF_8
): String
*/
