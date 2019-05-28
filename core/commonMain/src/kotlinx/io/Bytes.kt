package kotlinx.io

import kotlinx.io.buffer.*

typealias BytesPointer = Int

class Bytes : Closeable {
    private val buffers: MutableList<Buffer> = mutableListOf()
    private val limits: MutableList<Int> = mutableListOf()

    internal fun append(buffer: Buffer, limit: Int) {
        buffers.add(buffer)
        limits.add(limit)
    }

    internal fun discardFirst() {
        buffers.removeAt(0)
        limits.removeAt(0)
    }

    internal inline fun pointed(pointer: BytesPointer, consumer: (Int) -> Unit): Buffer {
        consumer(limits[pointer])
        return buffers[pointer]
    }

    internal inline fun advancePointer(pointer: BytesPointer): BytesPointer =
        pointer + 1

    internal inline fun isEmpty() =
        buffers.isEmpty()

    internal inline fun isAfterLast(index: BytesPointer) =
        index >= buffers.size

    fun size(): Int = limits.sum()

    fun input(): Input {
        return object : Input(this) {
            override fun closeSource() {}
            override fun fill(destination: Buffer, offset: Int, length: Int): Int = 0
        }
    }

    override fun toString() = "Bytes(${buffers.size} buffers)"

    override fun close() {
        // TODO: return buffers to the pool
    }

    companion object {
        const val InvalidPointer = Int.MIN_VALUE
        const val StartPointer = 0
    }

}

fun buildBytes(bufferSize: Int = DEFAULT_BUFFER_SIZE, builder: Output.() -> Unit): Bytes {
    return BytesOutput(bufferSize).apply(builder).bytes()
}

private class BytesOutput(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Output(bufferSize) {
    private val bytes = Bytes()

    fun bytes(): Bytes {
        close()
        return bytes
    }

    override fun flush(source: Buffer, length: Int): Int {
        bytes.append(source, length)
        return length
    }

    override fun close() {
        flush()
    }
}