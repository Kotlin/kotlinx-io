package kotlinx.io.core

import kotlinx.io.charsets.*
import org.khronos.webgl.*

actual fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String {
    if (offset < 0 || length < 0 || offset + length > bytes.size) {
        checkIndices(offset, length, bytes)
    }

    @Suppress("UnsafeCastFromDynamic")
    val i8: Int8Array = bytes.asDynamic() // we know that K/JS generates Int8Array for ByteBuffer
    val bufferOffset = i8.byteOffset + offset
    val buffer = i8.buffer.slice(bufferOffset, bufferOffset + length)

    val view = IoBuffer(buffer, null)
    view.resetForRead()
    val packet = ByteReadPacket(view, IoBuffer.NoPool)

    return charset.newDecoder().decode(packet, Int.MAX_VALUE)
}

fun checkIndices(offset: Int, length: Int, bytes: ByteArray): Nothing {
    require(offset >= 0) { throw IndexOutOfBoundsException("offset ($offset) shouldn't be negative") }
    require(length >= 0) { throw IndexOutOfBoundsException("length ($length) shouldn't be negative") }
    require(offset + length <= bytes.size) {
        throw IndexOutOfBoundsException("offset ($offset) + length ($length) > bytes.size (${bytes.size})")
    }

    throw IndexOutOfBoundsException()
}

internal actual fun String.getCharsInternal(dst: CharArray, dstOffset: Int) {
    val length = length
    require(dstOffset + length <= dst.size)

    var dstIndex = dstOffset
    for (srcIndex in 0 until length) {
        dst[dstIndex++] = this[srcIndex]
    }
}
