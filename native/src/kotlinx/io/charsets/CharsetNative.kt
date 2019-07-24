package kotlinx.io.charsets

import kotlinx.cinterop.*
import kotlinx.io.core.*
import platform.iconv.*
import platform.posix.*

actual abstract class Charset(internal val _name: String) {
    actual abstract fun newEncoder(): CharsetEncoder
    actual abstract fun newDecoder(): CharsetDecoder

    override fun toString(): String {
        return name
    }

    actual companion object {
        actual fun forName(name: String): Charset {
            if (name == "UTF-8" || name == "utf-8" || name == "UTF8" || name == "utf8") return Charsets.UTF_8
            if (name == "ISO-8859-1" || name == "iso-8859-1") return Charsets.ISO_8859_1
            if (name == "UTF-16" || name == "utf-16" || name == "UTF16" || name == "utf16") return Charsets.UTF_16

            return CharsetImpl(name)
        }
    }
}

private class CharsetImpl(name: String) : Charset(name) {
    init {
        val v = iconv_open(name, "UTF-8")
        checkErrors(v, name)
        iconv_close(v)
    }

    override fun newEncoder(): CharsetEncoder = CharsetEncoderImpl(this)
    override fun newDecoder(): CharsetDecoder = CharsetDecoderImpl(this)
}

actual val Charset.name: String get() = _name

// -----------------------

actual abstract class CharsetEncoder(internal val _charset: Charset)
private data class CharsetEncoderImpl(private val charset: Charset) : CharsetEncoder(charset)

actual val CharsetEncoder.charset: Charset get() = _charset

@SymbolName("Kotlin_Arrays_getShortArrayAddressOfElement")
private external fun getAddressOfElement(array: Any, index: Int): COpaquePointer

@Suppress("NOTHING_TO_INLINE")
private inline fun <P : CVariable> Pinned<*>.addressOfElement(index: Int): CPointer<P> =
    getAddressOfElement(this.get(), index).reinterpret()

private fun Pinned<CharArray>.addressOf(index: Int): CPointer<ByteVar> = this.addressOfElement(index)

private fun iconvCharsetName(name: String) = when (name) {
    "UTF-16" -> platformUtf16
    else -> name
}

@SharedImmutable
private val negativePointer = (-1L).toCPointer<IntVar>()

private fun checkErrors(iconvOpenResults: COpaquePointer?, charset: String) {
    if (iconvOpenResults == null || iconvOpenResults === negativePointer) {
        throw IllegalArgumentException("Failed to open iconv for charset $charset with error code ${posix_errno()}")
    }
}

actual fun CharsetEncoder.encodeToByteArray(input: CharSequence, fromIndex: Int, toIndex: Int): ByteArray =
    encodeToByteArrayImpl1(input, fromIndex, toIndex)

internal actual fun CharsetEncoder.encodeImpl(input: CharSequence, fromIndex: Int, toIndex: Int, dst: IoBuffer): Int {
    val length = toIndex - fromIndex
    if (length == 0) return 0

    val chars = CharArray(length)
    val charset = iconvCharsetName(_charset._name)
    val cd: COpaquePointer? = iconv_open(charset, platformUtf16)
    checkErrors(cd, charset)

    for (index in 0 .. length - 1) {
        chars[index] = input[fromIndex + index]
    }

    var charsConsumed = 0

    try {
        dst.writeDirect { buffer ->
            chars.usePinned { pinned ->
                memScoped {
                    val inbuf = alloc<CPointerVar<ByteVar>>()
                    val outbuf = alloc<CPointerVar<ByteVar>>()
                    val inbytesleft = alloc<size_tVar>()
                    val outbytesleft = alloc<size_tVar>()
                    val dstRemaining = dst.writeRemaining.convert<size_t>()

                    inbuf.value = pinned.addressOf(0)
                    outbuf.value = buffer
                    inbytesleft.value = (length * 2).convert<size_t>()
                    outbytesleft.value = dstRemaining

                    if (iconv(cd, inbuf.ptr, inbytesleft.ptr, outbuf.ptr, outbytesleft.ptr) == MAX_SIZE) {
                        checkIconvResult(posix_errno())
                    }

                    charsConsumed = ((length * 2).convert<size_t>() - inbytesleft.value).toInt() / 2
                    (dstRemaining - outbytesleft.value).toInt()
                }
            }
        }

        return charsConsumed
    } finally {
        iconv_close(cd)
    }
}

actual fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: Output) {
    val cd = iconv_open(charset.name, "UTF-8")
    checkErrors(cd, "UTF-8")

    try {
        var readSize = 1
        var writeSize = 1

        while (true) {
            val srcView = input.prepareRead(readSize)
            if (srcView == null) {
                if (readSize != 1) throw MalformedInputException("...")
                break
            }

            dst.writeWhileSize(writeSize) { dstBuffer ->
                var written: Int = 0

                dstBuffer.writeDirect { buffer ->
                    var read = 0

                    srcView.readDirect { src ->
                        memScoped {
                            val length = srcView.readRemaining.convert<size_t>()
                            val inbuf = alloc<CPointerVar<ByteVar>>()
                            val outbuf = alloc<CPointerVar<ByteVar>>()
                            val inbytesleft = alloc<size_tVar>()
                            val outbytesleft = alloc<size_tVar>()
                            val dstRemaining = dstBuffer.writeRemaining.convert<size_t>()

                            inbuf.value = src
                            outbuf.value = buffer
                            inbytesleft.value = length
                            outbytesleft.value = dstRemaining

                            if (iconv(cd, inbuf.ptr, inbytesleft.ptr, outbuf.ptr, outbytesleft.ptr) == MAX_SIZE) {
                                checkIconvResult(posix_errno())
                            }

                            read = (length - inbytesleft.value).toInt()
                            written = (dstRemaining - outbytesleft.value).toInt()
                        }

                        read
                    }

                    if (read == 0) {
                        readSize++
                        writeSize = 8
                    } else {
                        input.updateHeadRemaining(srcView.readRemaining)
                        readSize = 1
                        writeSize = 1
                    }

                    if (written > 0 && srcView.canRead()) writeSize else 0
                }
                written
            }
        }
    } finally {
        iconv_close(cd)
    }
}

private fun checkIconvResult(errno: Int) {
    if (errno == EILSEQ) throw MalformedInputException("Malformed or unmappable bytes at input")
    if (errno == EINVAL) return // too few input bytes
    if (errno == E2BIG) return // too few output buffer bytes

    throw IllegalStateException("Failed to call 'iconv' with error code ${errno}")
}

internal actual fun CharsetEncoder.encodeComplete(dst: IoBuffer): Boolean = true

// ----------------------------------------------------------------------

actual abstract class CharsetDecoder(internal val _charset: Charset)
private data class CharsetDecoderImpl(private val charset: Charset) : CharsetDecoder(charset)

actual val CharsetDecoder.charset: Charset get() = _charset

@SharedImmutable
private val platformUtf16: String = if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) "UTF-16BE" else "UTF-16LE"

actual fun CharsetDecoder.decode(input: Input, dst: Appendable, max: Int): Int {
    val charset = iconvCharsetName(charset.name)
    val cd = iconv_open(platformUtf16, charset)
    checkErrors(cd, charset)
    val chars = CharArray(8192)
    var copied = 0

    try {
        var readSize = 1

        chars.usePinned { pinned ->
            memScoped {
                val inbuf = alloc<CPointerVar<ByteVar>>()
                val outbuf = alloc<CPointerVar<ByteVar>>()
                val inbytesleft = alloc<size_tVar>()
                val outbytesleft = alloc<size_tVar>()

                val buffer = pinned.addressOf(0)

                input.takeWhileSize { srcView ->
                    val rem = max - copied
                    if (rem == 0) return@takeWhileSize 0

                    var written: Int = 0
                    var read = 0

                    srcView.readDirect { src ->
                        val length = srcView.readRemaining.convert<size_t>()
                        val dstRemaining = (minOf(chars.size, rem) * 2).convert<size_t>()

                        inbuf.value = src
                        outbuf.value = buffer
                        inbytesleft.value = length
                        outbytesleft.value = dstRemaining

                        if (iconv(cd, inbuf.ptr, inbytesleft.ptr, outbuf.ptr, outbytesleft.ptr) == MAX_SIZE) {
                            checkIconvResult(posix_errno())
                        }

                        read = (length - inbytesleft.value).toInt()
                        written = (dstRemaining - outbytesleft.value).toInt() / 2

                        read
                    }

                    if (read == 0) {
                        readSize++
                    } else {
                        readSize = 1

                        repeat(written) {
                            dst.append(chars[it])
                        }
                        copied += written
                    }

                    readSize
                }
            }
        }

        return copied
    } finally {
        iconv_close(cd)
    }
}

actual fun CharsetDecoder.decodeExactBytes(input: Input, inputLength: Int): String {
    if (inputLength == 0) return ""

    val charset = iconvCharsetName(charset.name)
    val cd = iconv_open(platformUtf16, charset)
    checkErrors(cd, charset)

    val chars = CharArray(inputLength)
    var charsCopied = 0
    var bytesConsumed = 0

    try {
        var readSize = 1

        chars.usePinned { pinned ->
            memScoped {
                val inbuf = alloc<CPointerVar<ByteVar>>()
                val outbuf = alloc<CPointerVar<ByteVar>>()
                val inbytesleft = alloc<size_tVar>()
                val outbytesleft = alloc<size_tVar>()

                input.takeWhileSize { srcView ->
                    val rem = inputLength - charsCopied
                    if (rem == 0) return@takeWhileSize 0

                    var written: Int = 0
                    var read = 0

                    srcView.readDirect { src ->
                        val length = minOf(srcView.readRemaining, inputLength - bytesConsumed).convert<size_t>()
                        val dstRemaining = (rem * 2).convert<size_t>()

                        inbuf.value = src
                        outbuf.value = pinned.addressOf(charsCopied)
                        inbytesleft.value = length
                        outbytesleft.value = dstRemaining

                        if (iconv(cd, inbuf.ptr, inbytesleft.ptr, outbuf.ptr, outbytesleft.ptr) == MAX_SIZE) {
                            checkIconvResult(posix_errno())
                        }

                        read = (length - inbytesleft.value).toInt()
                        written = (dstRemaining - outbytesleft.value).toInt() / 2

                        read
                    }

                    bytesConsumed += read

                    if (read == 0) {
                        readSize++
                    } else {
                        readSize = 1
                        charsCopied += written
                    }

                    if (bytesConsumed < inputLength) readSize else 0
                }
            }
        }

        return fromCharArray(chars, 0, charsCopied)
    } finally {
        iconv_close(cd)
    }
}

@SymbolName("Kotlin_String_fromCharArray")
private external fun fromCharArray(array: CharArray, start: Int, size: Int): String

// -----------------------------------------------------------

actual object Charsets {
    actual val UTF_8: Charset = CharsetImpl("UTF-8")
    actual val ISO_8859_1: Charset = CharsetImpl("ISO-8859-1")
    internal val UTF_16: Charset = CharsetImpl(platformUtf16)
}

actual class MalformedInputException actual constructor(message: String) : Throwable(message)
