package kotlinx.io.bytes

import kotlinx.io.*
import kotlinx.io.buffer.*
import kotlinx.io.pool.*

internal fun poolOfBuffers(bufferSize: Int = DEFAULT_BUFFER_SIZE): ObjectPool<Buffer> =
    if (bufferSize == DEFAULT_BUFFER_SIZE) {
        DefaultBufferPool.Instance
    } else {
        DefaultBufferPool(bufferSize)
    }

internal fun unmanagedPoolOfBuffers(bufferSize: Int = DEFAULT_BUFFER_SIZE): ObjectPool<Buffer> =
    if (bufferSize == DEFAULT_BUFFER_SIZE) {
        UnmanagedBufferPool.Instance
    } else {
        UnmanagedBufferPool(bufferSize)
    }

internal fun Bytes.createInput(bufferSize: Int = DEFAULT_BUFFER_SIZE): Input = BytesInput(
    this, bufferSize
)