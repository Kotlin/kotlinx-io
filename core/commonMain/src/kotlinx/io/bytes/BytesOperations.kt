package kotlinx.io.bytes

import kotlinx.io.buffer.*
import kotlin.contracts.*

/**
 * Create [BytesInput] with content from [block] using specified [bufferSize].
 */
fun buildInput(bufferSize: Int = DEFAULT_BUFFER_SIZE, block: BytesOutput.() -> Unit): BytesInput {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return BytesOutput(bufferSize).apply {
        block()
    }.createInput()
}
