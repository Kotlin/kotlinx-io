package kotlinx.io.browser

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

/**
 * A [RawSource] that reads data from an [ArrayBuffer].
 *
 * @param arrayBuffer the buffer to read from.
 */
public class ArrayBufferSource(
    public val arrayBuffer: ArrayBuffer,
) : RawSource {
    private var isClosed = false
    private val array = Int8Array(arrayBuffer)
    private var startIndex = 0.0

    override fun readAtMostTo(
        sink: Buffer,
        byteCount: Long,
    ): Long {
        require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
        check(!isClosed) { "Source is closed." }
        val available = getByteLength(array.buffer) - startIndex
        if (available <= 0) return -1L

        val readLength = minOf(byteCount, available.toLong())
        for (i in 0 until readLength) {
            sink.writeByte(getByte(array, startIndex + i))
        }
        startIndex += readLength
        return readLength
    }

    override fun close() {
        isClosed = true
    }
}
