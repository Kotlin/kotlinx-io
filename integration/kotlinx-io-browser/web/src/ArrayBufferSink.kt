package kotlinx.io.browser

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

/**
 * A [RawSink] that writes data to an [ArrayBuffer].
 *
 * @param arrayBuffer the buffer to write to.
 */
public class ArrayBufferSink(
    public val arrayBuffer: ArrayBuffer,
) : RawSink {

    /**
     * Creates a new [ArrayBufferSink] with a resizable [ArrayBuffer] of [maxByteLength].
     *
     * @param maxByteLength the maximum capacity of the underlying buffer.
     */
    public constructor(maxByteLength: Long) : this(ArrayBuffer(0.0, maxByteLength.toDouble()))

    private var isClosed = false
    private val array = Int8Array(arrayBuffer)

    override fun write(
        source: Buffer,
        byteCount: Long,
    ) {
        require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
        check(!isClosed) { "Sink is closed." }
        val startIndex = getByteLength(array.buffer)
        resize(array.buffer, startIndex + byteCount)
        for (i in 0 until byteCount) {
            setByte(array, startIndex + i, source.readByte())
        }
    }

    override fun flush() {
        // Nothing to do
    }

    override fun close() {
        isClosed = true
    }
}
