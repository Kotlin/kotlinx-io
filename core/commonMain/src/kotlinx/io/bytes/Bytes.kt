package kotlinx.io.bytes

import kotlinx.io.buffer.*
import kotlin.contracts.*

/**
 * Byte sequence on top of Array of [Buffer].
 */
internal class Bytes {
    var buffers: Array<Buffer?> = arrayOfNulls(INITIAL_PREVIEW_SIZE)

    /**
     * Limit of each [Buffer] in [buffers].
     */
    var limits: IntArray = IntArray(INITIAL_PREVIEW_SIZE)

    /**
     * Index of the first [Buffer] in [buffers].
     */
    var head: Int = 0

    /**
     * Index of the last [Buffer] in [buffers].
     */
    private var tail: Int = 0

    /**
     * Calculate size of [Bytes].
     */
    fun size(): Int = size(0)

    /**
     * Check if [Bytes] is empty.
     */
    fun isEmpty(): Boolean = tail == head

    fun append(buffer: Buffer, limit: Int) {
        /**
         * Drop head buffers if we had discard operations.
         */
        if (head > 0) {
            compactBuffers()
        }

        if (tail == buffers.size) {
            buffers = buffers.copyInto(arrayOfNulls(buffers.size * 2))
            limits = limits.copyInto(IntArray(buffers.size * 2))
        }

        buffers[tail] = buffer
        limits[tail] = limit
        tail++
    }

    fun discardFirst() {
        check (head != tail) { "Bytes is empty." }

        buffers[head] = null
        limits[head] = -1
        head++
    }

    fun limit(pointer: Int): Int = limits[pointer + head]

    fun isAfterLast(index: Int): Boolean = head + index >= tail

    /**
     * Compute the size from [pointer] to the end.
     *
     * --- Please consider about storing size in local in public primitives. ---
     */
    fun size(pointer: Int): Int {
        var sum = 0
        for (index in (pointer + head) until tail) {
            sum += limits[index]
        }

        return sum
    }

    /**
     * Compact arrays after several discards.
     */
    private fun compactBuffers() {
        buffers.copyInto(buffers, 0, head, tail)
        limits.copyInto(limits, 0, head, tail)
        tail -= head
        head = 0
    }


    /**
     * Create bytes snapshot without copying content of [buffers].
     */
    fun snapshot(): Bytes = Bytes().also {
        if (head > 0) {
            compactBuffers()
        }

        it.buffers = buffers.copyInto(arrayOfNulls(buffers.size))
        it.limits = limits.copyInto(IntArray(limits.size))
        it.tail = tail
    }

    override fun toString() = "Bytes($head..$tail)"

    companion object {
        private const val INITIAL_PREVIEW_SIZE = 1
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