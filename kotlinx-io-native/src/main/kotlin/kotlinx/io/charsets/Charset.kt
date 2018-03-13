package kotlinx.io.charsets

import kotlinx.io.core.*

import kotlinx.cinterop.*
import kotlinx.io.pool.*
import platform.posix.memcpy
import platform.posix.memset
import platform.posix.size_t
import platform.posix.size_tVar
import platform.linux.iconv_open
import platform.linux.iconv_close
import platform.linux.iconv

import konan.SymbolName

actual abstract class Charset(internal val _name: String) {
    actual abstract fun newEncoder(): CharsetEncoder
    actual abstract fun newDecoder(): CharsetDecoder

    actual companion object {
        actual fun forName(name: String): Charset {
            if (name == "UTF-8" || name == "utf-8" || name == "UTF8" || name == "utf8") return Charsets.UTF_8
            throw IllegalArgumentException("Charset $name is not supported")
        }
    }
}

private class CharsetImpl(name: String) : Charset(name) {
    init {
        val v = iconv_open(name, "UTF-8")
        //if (v == -1) throw IllegalArgumentException("Charset $name is not supported")
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

@SymbolName("Kotlin_Arrays_getAddressOfElement")
private external fun getAddressOfElement(array: Any, index: Int): COpaquePointer

@Suppress("NOTHING_TO_INLINE")
private inline fun <P : CVariable> Pinned<*>.addressOfElement(index: Int): CPointer<P> =
        getAddressOfElement(this.get(), index).reinterpret()

private fun Pinned<CharArray>.addressOf(index: Int): CPointer<ByteVar> = this.addressOfElement(index)

internal actual fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int, toIndex: Int, dst: BufferView): Int {
    val length = toIndex - fromIndex
    val chars = CharArray(length) { input[fromIndex + it] }
    val cd = iconv_open(_charset.name, "UTF-16")
    //if (cd.reinterpret<Int> == -1) throw IllegalArgumentException("failed to open iconv")
    var charsConsumed = 0

    try {
        dst.writeDirect { buffer ->
            chars.usePinned { pinned ->
                memScoped {
                    val inbuf = alloc<CPointerVar<ByteVar>>()
                    val outbuf = alloc<CPointerVar<ByteVar>>()
                    val inbytesleft = alloc<size_tVar>()
                    val outbytesleft = alloc<size_tVar>()
                    val dstRemaining = dst.writeRemaining.toLong()

                    inbuf.value = pinned.addressOf(0)
                    outbuf.value = buffer
                    inbytesleft.value = (length * 2).toLong()
                    outbytesleft.value = dstRemaining

                    iconv(cd, inbuf.ptr, inbytesleft.ptr, outbuf.ptr, outbytesleft.ptr)

                    charsConsumed = (length * 2 - inbytesleft.value).toInt() / 2

                    (dstRemaining - outbytesleft.value).toInt()
                }
            }
        }

        return charsConsumed
    } finally {
        iconv_close(cd)
    }
}

actual fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: BytePacketBuilder) {
    val cd = iconv_open(charset.name, "UTF-8")
    //if (cd.reinterpret<Int> == -1) throw IllegalArgumentException("failed to open iconv")

    try {
        var readSize = 1
        var writeSize = 1

        while (true) {
            val srcView = input.prepareRead(readSize)
            if (srcView == null) {
                if (readSize != 1) throw MalformedInputException("...")
                break
            }

            dst.write(writeSize) { dstBuffer ->
                var written: Int = 0

                dstBuffer.writeDirect { buffer ->
                    var read = 0

                    srcView.readDirect { src ->
                        memScoped {
                            val length = srcView.readRemaining.toLong()
                            val inbuf = alloc<CPointerVar<ByteVar>>()
                            val outbuf = alloc<CPointerVar<ByteVar>>()
                            val inbytesleft = alloc<size_tVar>()
                            val outbytesleft = alloc<size_tVar>()
                            val dstRemaining = dstBuffer.writeRemaining.toLong()

                            inbuf.value = src
                            outbuf.value = buffer
                            inbytesleft.value = length
                            outbytesleft.value = dstRemaining

                            iconv(cd, inbuf.ptr, inbytesleft.ptr, outbuf.ptr, outbytesleft.ptr)

                            read = (length - inbytesleft.value).toInt()
                            written = (dstRemaining - outbytesleft.value).toInt()
                        }

                        read
                    }

                    if (read == 0) {
                        readSize++
                        writeSize = 8
                    } else {
                        @Suppress("DEPRECATION_ERROR")
                        input.`$updateRemaining$`(srcView.readRemaining)
                        readSize = 1
                        writeSize = 1
                    }

                    written
                }
                written
            }
        }
    } finally {
        iconv_close(cd)
    }
}

internal actual fun CharsetEncoder.encodeComplete(dst: BufferView): Boolean = true

// ----------------------------------------------------------------------

actual abstract class CharsetDecoder(internal val _charset: Charset)
private data class CharsetDecoderImpl(private val charset: Charset) : CharsetDecoder(charset)

actual val CharsetDecoder.charset: Charset get() = _charset

private val platformUtf16: String by lazy { if (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN) "UTF-16BE" else "UTF-16LE" }

actual fun CharsetDecoder.decode(input: ByteReadPacket, dst: Appendable, max: Int): Int {
    val cd = iconv_open(platformUtf16, charset.name)
    //if (cd.reinterpret<Int> == -1) throw IllegalArgumentException("failed to open iconv")
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
                        val length = srcView.readRemaining.toLong()
                        val dstRemaining = minOf(chars.size, rem).toLong() * 2L

                        inbuf.value = src
                        outbuf.value = buffer
                        inbytesleft.value = length
                        outbytesleft.value = dstRemaining

                        iconv(cd, inbuf.ptr, inbytesleft.ptr, outbuf.ptr, outbytesleft.ptr)

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

// -----------------------------------------------------------

actual object Charsets {
    actual val UTF_8: Charset = CharsetImpl("UTF-8")
}

actual class MalformedInputException actual constructor(message: String) : Throwable(message)
