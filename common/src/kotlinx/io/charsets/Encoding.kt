package kotlinx.io.charsets

import kotlinx.io.core.*
import kotlin.native.concurrent.*

expect abstract class Charset {
    @ExperimentalIoApi
    abstract fun newEncoder(): CharsetEncoder

    @ExperimentalIoApi
    abstract fun newDecoder(): CharsetDecoder

    companion object {
        fun forName(name: String): Charset
    }
}

expect val Charset.name: String

// ----------------------------- ENCODER -------------------------------------------------------------------------------
@ExperimentalIoApi
expect abstract class CharsetEncoder

expect val CharsetEncoder.charset: Charset

@Deprecated(
    "Use writeText on Output instead.",
    ReplaceWith("dst.writeText(input, fromIndex, toIndex, charset)", "kotlinx.io.core.writeText")
)
fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int, toIndex: Int, dst: Output) {
     encodeToImpl(dst, input, fromIndex, toIndex)
}

@SharedImmutable
private val EmptyByteArray = ByteArray(0)

@ExperimentalIoApi
expect fun CharsetEncoder.encodeToByteArray(input: CharSequence,
                                            fromIndex: Int = 0,
                                            toIndex: Int = input.length): ByteArray

@Deprecated(
    "Internal API. Will be hidden in future releases. Use encodeToByteArray instead.",
    replaceWith = ReplaceWith("encodeToByteArray(input, fromIndex, toIndex)")
)
fun CharsetEncoder.encodeToByteArrayImpl(
    input: CharSequence,
    fromIndex: Int = 0,
    toIndex: Int = input.length
): ByteArray {
    return encodeToByteArray(input, fromIndex, toIndex)
}

@ExperimentalIoApi
expect fun CharsetEncoder.encodeUTF8(input: ByteReadPacket, dst: Output)

@ExperimentalIoApi
fun CharsetEncoder.encode(input: CharSequence, fromIndex: Int = 0, toIndex: Int = input.length) = buildPacket {
    encodeToImpl(this, input, fromIndex, toIndex)
}

@ExperimentalIoApi
fun CharsetEncoder.encodeUTF8(input: ByteReadPacket) = buildPacket {
    encodeUTF8(input, this)
}

// ----------------------------- DECODER -------------------------------------------------------------------------------

@ExperimentalIoApi
expect abstract class CharsetDecoder

/**
 * Decoder's charset it is created for.
 */
expect val CharsetDecoder.charset: Charset

@ExperimentalIoApi
fun CharsetDecoder.decode(input: Input, max: Int = Int.MAX_VALUE): String = buildString(minOf(max.toLong(), input.sizeEstimate()).toInt()) {
    decode(input, this, max)
}

@ExperimentalIoApi
expect fun CharsetDecoder.decode(input: Input, dst: Appendable, max: Int): Int

@ExperimentalIoApi
expect fun CharsetDecoder.decodeExactBytes(input: Input, inputLength: Int): String

// ----------------------------- REGISTRY ------------------------------------------------------------------------------
expect object Charsets {
    val UTF_8: Charset
    val ISO_8859_1: Charset
}

expect class MalformedInputException(message: String) : Throwable







// ----------------------------- INTERNALS -----------------------------------------------------------------------------


internal expect fun CharsetEncoder.encodeImpl(input: CharSequence, fromIndex: Int, toIndex: Int, dst: IoBuffer): Int

internal expect fun CharsetEncoder.encodeComplete(dst: IoBuffer): Boolean

internal fun CharsetEncoder.encodeToByteArrayImpl1(
    input: CharSequence,
    fromIndex: Int = 0,
    toIndex: Int = input.length
): ByteArray {
    var start = fromIndex
    if (start >= toIndex) return EmptyByteArray
    val single = IoBuffer.Pool.borrow()

    try {
        IoBuffer.NoPool
        val rc = encodeImpl(input, start, toIndex, single)
        start += rc
        if (start == toIndex) {
            val result = ByteArray(single.readRemaining)
            single.readFully(result)
            return result
        }

        val builder = BytePacketBuilder(0, IoBuffer.Pool)
        builder.last(single.makeView())
        encodeToImpl(builder, input, start, toIndex)
        return builder.build().readBytes()
    } finally {
        single.release(IoBuffer.Pool)
    }
}

internal fun Input.sizeEstimate(): Long = when (this) {
    is ByteReadPacket -> remaining
    is ByteReadPacketBase -> maxOf(remaining, 16)
    else -> 16
}


private fun CharsetEncoder.encodeCompleteImpl(dst: Output): Int {
    var size = 1
    var bytesWritten = 0

    dst.writeWhile { view ->
        val before = view.writeRemaining
        if (encodeComplete(view)) {
            size = 0
        } else {
            size++
        }
        bytesWritten += before - view.writeRemaining
        size > 0
    }

    return bytesWritten
}


internal fun CharsetEncoder.encodeToImpl(
    destination: Output,
    input: CharSequence,
    fromIndex: Int,
    toIndex: Int
): Int {
    var start = fromIndex
    if (start >= toIndex) return 0

    var bytesWritten = 0

    destination.writeWhileSize(1) { view: IoBuffer ->
        val before = view.writeRemaining
        val rc = encodeImpl(input, start, toIndex, view)
        check(rc >= 0)
        start += rc
        bytesWritten += before - view.writeRemaining

        when {
            start >= toIndex -> 0
            rc == 0 -> 8
            else -> 1
        }
    }

    bytesWritten += encodeCompleteImpl(destination)
    return bytesWritten
}
