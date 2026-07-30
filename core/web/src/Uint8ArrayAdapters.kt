@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.io

import kotlinx.io.external.ArrayBuffer
import kotlinx.io.external.Uint8Array
import kotlinx.io.external.get
import kotlinx.io.external.set
import kotlin.js.JsException

/**
 * Returns a [RawSink] which writes to this [Uint8Array]. Each call to [write] resizes the underlying [ArrayBuffer] and
 * will throw an [IOException] if the resize fails.
 */
public fun Uint8Array.asSink(): RawSink = object : RawSink {

    private var isClosed = false

    override fun write(
        source: Buffer,
        byteCount: Long,
    ) {
        require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
        check(!isClosed) { "Sink is closed." }
        val startIndex = length
        try {
            buffer.resize(startIndex + byteCount)
        } catch (e: JsException) {
            throw IOException("Failed to resize buffer", e)
        }
        for (i in 0 until byteCount) {
            set(startIndex + i, source.readByte())
        }
    }

    override fun flush() {
        check(!isClosed) { "Sink is closed." }
        // Nothing to do
    }

    override fun close() {
        isClosed = true
    }
}

/** Returns a [RawSource] which reads from this [Uint8Array]. */
public fun Uint8Array.asSource(): RawSource = object : RawSource {
    private var isClosed = false
    private var startIndex = 0.0

    override fun readAtMostTo(
        sink: Buffer,
        byteCount: Long,
    ): Long {
        require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
        check(!isClosed) { "Source is closed." }
        val available = length - startIndex
        if (available <= 0) return -1L

        val readLength = minOf(byteCount, available.toLong())
        for (i in 0 until readLength) {
            sink.writeByte(get(startIndex + i))
        }
        startIndex += readLength
        return readLength
    }

    override fun close() {
        isClosed = true
    }
}
