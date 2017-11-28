package kotlinx.io.js

import kotlinx.io.core.*
import org.khronos.webgl.*

fun ByteReadPacket.readText(encoding: String, max: Int = Int.MAX_VALUE): String = buildString(remaining) {
    readText(encoding, this, max)
}

fun ByteReadPacket.readText(encoding: String = "UTF-8", out: Appendable, max: Int = Int.MAX_VALUE): Int {
    require(max >= 0) { "max shouldn't be negative, got $max"}
    val decoder = TextDecoderFatal(encoding)
    var decoded = 0

    while (decoded < max) {
        @Suppress("INVISIBLE_MEMBER")
        readDirect { view: BufferView ->
            decoded += view.readText(decoder, out, view.next == null, max - decoded)
        }

        if (isEmpty) break
    }

    return decoded
}

internal external class TextDecoder(encoding: String, options: dynamic = definedExternally) {
    val encoding: String

    fun decode(): String
    fun decode(buffer: ArrayBuffer): String
    fun decode(buffer: ArrayBuffer, options: dynamic): String
    fun decode(buffer: ArrayBufferView): String
    fun decode(buffer: ArrayBufferView, options: dynamic): String
}

internal external class TextEncoder(encoding: String) {
    fun encode()
}

private val STREAM_TRUE = Any().apply {
    with(this.asDynamic()) {
        stream = true
    }
}

private val FATAL_TRUE = Any().apply {
    with(this.asDynamic()) {
        fatal = true
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun TextDecoderFatal(encoding: String, fatal: Boolean = true): TextDecoder {
    return if (fatal) TextDecoder(encoding, FATAL_TRUE) else TextDecoder(encoding)
}

internal inline fun TextDecoder.decodeStream(buffer: ArrayBufferView, stream: Boolean): String {
    return if (stream) {
        decode(buffer, STREAM_TRUE)
    } else {
        decode(buffer)
    }
}
