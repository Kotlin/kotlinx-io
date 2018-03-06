package kotlinx.io.js

import kotlinx.io.core.*
import org.khronos.webgl.*

fun ByteReadPacket.readText(encoding: String, max: Int = Int.MAX_VALUE): String = buildString(minOf(max, remaining.coerceAtMostMaxInt())) {
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

internal fun TextDecoderFatal(encoding: String, fatal: Boolean = true): TextDecoder {
    // PhantomJS does not support TextDecoder yet so we use node module text-encoding for tests
    if (js("typeof TextDecoder") == "undefined") {
        val module = js("require('text-encoding')")
        if (module.TextDecoder === undefined) throw IllegalStateException("TextDecoder is not supported by your browser and no text-encoding module found")
        val ctor = module.TextDecoder
        val objPrototype = js("Object").create(ctor.prototype)

        @Suppress("UnsafeCastFromDynamic")
        return if (fatal) ctor.call(objPrototype, encoding, FATAL_TRUE)
        else ctor.call(objPrototype, encoding)
    }

    return if (fatal) TextDecoder(encoding, FATAL_TRUE) else TextDecoder(encoding)
}

internal inline fun TextDecoder.decodeStream(buffer: ArrayBufferView, stream: Boolean): String {
    return if (stream) {
        decode(buffer, STREAM_TRUE)
    } else {
        decode(buffer)
    }
}


internal inline fun TextDecoder.decodeStream(buffer: ArrayBuffer, stream: Boolean): String {
    return if (stream) {
        decode(buffer, STREAM_TRUE)
    } else {
        decode(buffer)
    }
}
