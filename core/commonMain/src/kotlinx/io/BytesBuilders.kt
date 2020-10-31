package kotlinx.io

import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.DEFAULT_BUFFER_SIZE
import kotlinx.io.buffer.DefaultBufferPool
import kotlinx.io.buffer.compact
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Create [Bytes] with [bufferSize] and fills it from [builder].
 */
public inline fun buildBytes(bufferSize: Int = DEFAULT_BUFFER_SIZE, builder: Output.() -> Unit): Bytes {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return BytesOutput(bufferSize).apply(builder).bytes()
}

@PublishedApi
internal class BytesOutput(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Output(
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
