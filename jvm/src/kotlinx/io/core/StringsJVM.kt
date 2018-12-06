package kotlinx.io.core

import kotlinx.io.charsets.*

@Suppress("NOTHING_TO_INLINE")
actual inline fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String =
        java.lang.String(bytes, offset, length, charset) as String


internal actual fun String.getCharsInternal(dst: CharArray, dstOffset: Int) {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    (this as java.lang.String).getChars(0, length, dst, dstOffset)
}
