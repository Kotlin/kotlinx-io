package kotlinx.io

import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.DEFAULT_BUFFER_SIZE
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Create [Bytes] with [bufferSize] and fills it from [builder].
 */
fun buildBytes(bufferSize: Int = DEFAULT_BUFFER_SIZE, builder: Output.() -> Unit): Bytes {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return BytesOutput(bufferSize).apply(builder).bytes()
}

private class BytesOutput(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Output(bufferSize) {
    private val bytes = Bytes(bufferPool)

    fun bytes(): Bytes {
        close()
        return bytes
    }

    override fun flush(source: Buffer, length: Int) {
        bytes.append(source, length)
    }

    override fun closeSource() {
        // Nothing, cannot be closed
    }
}
