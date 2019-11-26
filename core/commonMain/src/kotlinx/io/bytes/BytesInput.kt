package kotlinx.io.bytes

import kotlinx.io.*
import kotlinx.io.buffer.*

/**
 * In memory byte source created from [BytesOutput] or by using [buildInput].
 *
 * [BytesInput] isn't pooled so it's safe to throw it away.
 */
public class BytesInput internal constructor(
    bytes: Bytes,
    bufferSize: Int
) : Input(bytes, unmanagedPoolOfBuffers(bufferSize)) {

    /**
     * Count of remaining bytes in [BytesInput].
     */
    public val remaining: Int
        get() = size()

    override fun fill(buffer: Buffer): Int {
        return 0
    }

    override fun closeSource() {
    }
}
