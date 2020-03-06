package kotlinx.io.bytes

import kotlinx.io.*
import kotlinx.io.buffer.*

/**
 * Create unlimited [Output] stored in memory.
 * In advance to [Output] you can check [size] and create [BytesInput] with [createInput].
 * [BytesOutput] isn't using any pools and shouldn't be closed.
 */
public class BytesOutput(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : Output(unmanagedPoolOfBuffers(bufferSize)) {
    private val bytes = Bytes()

    /**
     * Size of content.
     */
    public val size: Int get() = bytes.size() + size()

    /**
     * Create [BytesInput] from this [Output].
     * This can be called multiple times and. It always returns independent [BytesInput] without copying of underline buffers.
     * The buffers will be copied on demand.
     *
     * [BytesOutput] is safe to append content after [createInput]. It won't change any already created [BytesInput].
     */
    public fun createInput(): BytesInput {
        if (size() > 0) {
            flush()
        }

        return BytesInput(bytes.snapshot(), bufferSize)
    }

    override fun flush(source: Buffer, startIndex: Int, endIndex: Int): Boolean {
        if (startIndex > 0) {
            source.compact(startIndex, endIndex)
        }
        bytes.append(source, endIndex)

        return false
    }

    override fun closeSource() {
        // No source to close.
    }
}
