package kotlinx.io.charsets

import kotlinx.io.core.*
import kotlinx.io.core.internal.*

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

fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int, toIndex: Int, dst: Output) {
    var start = fromIndex

    if (start >= toIndex) return
    dst.writeWhileSize(1) { view: Buffer ->
        val rc = encodeImpl(input, start, toIndex, view)
        check(rc >= 0)
        start += rc

        when {
            start >= toIndex -> 0
            rc == 0 -> 8
            else -> 1
        }
    }

    encodeCompleteImpl(dst)
}

private val EmptyByteArray = ByteArray(0)

expect fun CharsetEncoder.encodeToByteArray(input: CharSequence,
                                            fromIndex: Int = 0,
                                            toIndex: Int = input.length): ByteArray

@DangerousInternalIoApi
fun CharsetEncoder.encodeToByteArrayImpl(input: CharSequence,
                                                  fromIndex: Int = 0,
                                                  toIndex: Int = input.length): ByteArray {
    var start = fromIndex
    if (start >= toIndex) return EmptyByteArray
    val single = ChunkBuffer.Pool.borrow()

    try {
        val rc = encodeImpl(input, start, toIndex, single)
        start += rc
        if (start == toIndex) {
            val result = ByteArray(single.readRemaining)
            single.readFully(result)
            return result
        }

        val builder = BytePacketBuilder(0, ChunkBuffer.Pool)
        builder.last(single.duplicate())
        encode(input, start, toIndex, builder)
        return builder.build().readBytes()
    } finally {
        single.release(ChunkBuffer.Pool)
    }
}

expect fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: Output)

private fun CharsetEncoder.encodeCompleteImpl(dst: Output) {
    var size = 1
    dst.writeWhile { view ->
        if (encodeComplete(view)) {
            size = 0
        } else {
            size++
        }
        size > 0
    }
}

fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int = 0, toIndex: Int = input.length) = buildPacket {
    encode(input, fromIndex, toIndex, this)
}

fun CharsetEncoder.encodeUTF8(input: ByteReadPacket) = buildPacket {
    encodeUTF8(input, this)
}

internal expect fun CharsetEncoder.encodeImpl(input: CharSequence, fromIndex: Int, toIndex: Int, dst: Buffer): Int
internal expect fun CharsetEncoder.encodeComplete(dst: Buffer): Boolean

// ----------------------------- DECODER -------------------------------------------------------------------------------

expect abstract class CharsetDecoder
expect val CharsetDecoder.charset: Charset

fun CharsetDecoder.decode(input: Input, max: Int = Int.MAX_VALUE): String = buildString(minOf(max.toLong(), input.sizeEstimate()).toInt()) {
    decode(input, this, max)
}

internal fun Input.sizeEstimate(): Long = when (this) {
    is ByteReadPacket -> remaining
    is AbstractInput -> maxOf(remaining, 16)
    else -> 16
}

expect fun CharsetDecoder.decode(input: Input, dst: Appendable, max: Int): Int

expect fun CharsetDecoder.decodeExactBytes(input: Input, inputLength: Int): String

// ----------------------------- REGISTRY ------------------------------------------------------------------------------
expect object Charsets {
    val UTF_8: Charset
}

expect class MalformedInputException(message: String) : Throwable

