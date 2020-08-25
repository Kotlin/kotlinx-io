package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.contracts.*

/**
 * Create [Bytes] with [bufferSize] and fills it from [builder].
 */
public fun buildBytes(bufferSize: Int = DEFAULT_BUFFER_SIZE, builder: Output.() -> Unit): Bytes {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return BytesOutput(bufferSize).apply(builder).bytes()
}

private class BytesOutput(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Output(
    if (bufferSize == DEFAULT_BUFFER_SIZE) DefaultBufferPool.Instance else DefaultBufferPool(bufferSize)
) {
    private val bytes = Bytes(pool)

    fun bytes(): Bytes {
        close()
        return bytes
    }

    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
        val length = source.compact(startIndex, endIndex)
        bytes.append(source, length)
    }

    override fun closeSource() {
        // Nothing, cannot be closed
    }
}
