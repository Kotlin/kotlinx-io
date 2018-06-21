package kotlinx.io.core

import kotlinx.io.charsets.*

@Suppress("NOTHING_TO_INLINE")
actual inline fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String =
        java.lang.String(bytes, offset, length, charset) as String

