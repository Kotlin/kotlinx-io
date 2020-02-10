package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.pool.*
import kotlin.contracts.*

internal typealias BytesPointer = Int

/**
 * Read-only bytes container.
 *
 * Use [input] to create readable [Input].
 * All inputs from a [Bytes] instance are share the same buffers.
 *
 * ```
 * buildBytes {
 * }
 * ```
 * 1. create
 * 2. close
 * 3. example
 */
class Bytes internal constructor(internal val bufferPool: ObjectPool<Buffer>) : Closeable {
    internal var buffers: Array<Buffer?> = arrayOfNulls(initialPreviewSize)
    internal var limits: IntArray = IntArray(initialPreviewSize)
    internal var head: Int = 0
    internal var tail: Int = 0

    /**
     * Calculate size of [Bytes].
     */
    fun size(): Int = size(StartPointer)

    /**
     * Create [Input] view on content.
     */
    fun input(): Input = object : Input(this@Bytes, bufferPool) {
        override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int = 0
        override fun closeSource() {}
    }

    override fun toString() = "Bytes($head..$tail)"

    /**
     * Release all data. Every produced input is broken.
     */
    override fun close(): Unit {
        (head until tail).forEach {
            bufferPool.recycle(buffers[it]!!)
            buffers[it] = null
        }
        head = 0
        tail = 0
    }

    internal fun append(buffer: Buffer, limit: Int): Unit {
        if (head > 0) {
            // if we are appending buffers after some were discarded, 
            // compact arrays so we can store more without allocations
            buffers.copyInto(buffers, 0, head, tail)
            limits.copyInto(limits, 0, head, tail)
            tail -= head
            head = 0
        }

        if (tail == buffers.size) {
            buffers = buffers.copyInto(arrayOfNulls(buffers.size * 2))
            limits = limits.copyInto(IntArray(buffers.size * 2))
        }

        buffers[tail] = buffer
        limits[tail] = limit
        tail++
    }

    internal fun discardFirst(): Unit {
        if (head == tail)
            throw NoSuchElementException("There is no buffer to discard in this instance")
        buffers[head] = null
        limits[head] = -1
        head++
    }

    internal fun limit(pointer: BytesPointer): Int = limits[pointer + head]

    internal fun isEmpty() = tail == head

    internal fun isAfterLast(index: BytesPointer) = head + index >= tail

    internal fun size(pointer: BytesPointer): Int {
        var sum = 0
        for (index in (pointer + head) until tail) {
            sum += limits[index]
        }
        return sum
    }

    companion object {
        internal const val InvalidPointer = Int.MIN_VALUE
        internal const val StartPointer = 0

        private const val initialPreviewSize = 1
    }
}

/**
 * Get [Buffer] and limit according to [pointer] offset in [Bytes.buffers].
 */
internal inline fun Bytes.pointed(pointer: Int, consumer: (buffer: Buffer, limit: Int) -> Unit) {
    contract {
        callsInPlace(consumer, kind = InvocationKind.EXACTLY_ONCE)
    }

    val index = pointer + head
    val buffer = buffers[index] ?: throw NoSuchElementException("There is no buffer at pointer $pointer.")
    val limit = limits[index]
    consumer(buffer, limit)
}
