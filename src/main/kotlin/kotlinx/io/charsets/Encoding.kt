package kotlinx.io.charsets

import kotlinx.io.core.*

expect abstract class Charset {
    abstract fun newEncoder(): CharsetEncoder
    abstract fun newDecoder(): CharsetDecoder

    companion object {
        fun forName(name: String): Charset
    }
}

expect val Charset.name: String

// ----------------------------- ENCODER -------------------------------------------------------------------------------
expect abstract class CharsetEncoder

expect val CharsetEncoder.charset: Charset

fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int, toIndex: Int, dst: BytePacketBuilder) {
    var start = fromIndex
    var size = 1

    while (start < toIndex) {
        dst.write(size) { view ->
            val before = view.writeRemaining
            val rc = encode(input, start, toIndex, view)
            start += rc
            size = if (rc == 0) 8 else 1
            before - view.writeRemaining
        }
    }

    encodeCompleteImpl(dst)
}

expect fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: BytePacketBuilder)

private fun CharsetEncoder.encodeCompleteImpl(dst: BytePacketBuilder) {
    var size = 1
    while (size > 0) {
        dst.write(size) { view ->
            val before = view.readRemaining

            if (encodeComplete(view)) {
                size = 0
            } else {
                size++
            }

            before - view.readRemaining
        }
    }
}

fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int = 0, toIndex: Int = input.length) = buildPacket {
    encode(input, fromIndex, toIndex, this)
}

fun CharsetEncoder.encodeUTF8(input: ByteReadPacket) = buildPacket {
    encodeUTF8(input, this)
}

internal expect fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int, toIndex: Int, dst: BufferView): Int
internal expect fun CharsetEncoder.encodeComplete(dst: BufferView): Boolean

// ----------------------------- DECODER -------------------------------------------------------------------------------

expect abstract class CharsetDecoder
expect val CharsetDecoder.charset: Charset

fun CharsetDecoder.decode(input: Input, max: Int = Int.MAX_VALUE): String = buildString(minOf(max.toLong(), input.sizeEstimate()).toInt()) {
    decode(input, this, max)
}

internal fun Input.sizeEstimate(): Long = when (this) {
    is ByteReadPacket -> remaining
    is ByteReadPacketBase -> remaining
    else -> 16
}

expect fun CharsetDecoder.decode(input: Input, dst: Appendable, max: Int): Int

// ----------------------------- REGISTRY ------------------------------------------------------------------------------
expect object Charsets {
    val UTF_8: Charset
}

expect class MalformedInputException(message: String) : Throwable

