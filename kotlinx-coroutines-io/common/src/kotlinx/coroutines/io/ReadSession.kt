package kotlinx.coroutines.io

import kotlinx.io.bits.Memory
import kotlinx.io.core.*
import kotlinx.io.errors.TODO_ERROR

/*
@ExperimentalIoApi
suspend inline fun ByteReadChannel.readMemory(
    desiredSize: Int = 1,
    block: (Memory, start: Int, endExclusive: Int) -> Int
): Int {
    val buffer = requestBuffer(desiredSize) ?: Buffer.Empty
    var bytesRead = 0
    try {
        bytesRead = block(buffer.memory, buffer.readPosition, buffer.readRemaining)
        return bytesRead
    } finally {
        completeReadingFromBuffer(buffer, bytesRead)
    }
}

@PublishedApi
internal fun ByteReadChannel.requestBuffer(desiredSize: Int): Buffer? {
    @Suppress("DEPRECATION")
    if (this is ReadSession) {
        return request(desiredSize.coerceAtMost(Buffer.ReservedSize))
    }

    TODO_ERROR("!!!")
}

@PublishedApi
internal fun ByteReadChannel.completeReadingFromBuffer(buffer: Buffer?, bytesRead: Int) {
    @Suppress("DEPRECATION")
    if (this is ReadSession) {
        discard(bytesRead)
        return
    }


}
*/

@Deprecated("Use readMemory instead.")
interface ReadSession {
    /**
     * Number of bytes available for read. However it doesn't necessarily means that all available bytes could be
     * requested at once
     */
    val availableForRead: Int

    /**
     * Discard at most [n] available bytes or 0 if no bytes available yet
     * @return number of bytes actually discarded, could be 0
     */
    fun discard(n: Int): Int

    /**
     * Request buffer range [atLeast] bytes length
     *
     * There are the following reasons for this function to return `null`:
     * - not enough bytes available yet (should be at least `atLeast` bytes available)
     * - due to buffer fragmentation it is impossible to represent the requested range as a single buffer range
     * - end of stream encountered and all bytes were consumed
     *
     * @return buffer for the requested range or `null` if it is impossible to provide such a buffer view
     * @throws Throwable if the channel has been closed with an exception or cancelled
     */
    fun request(atLeast: Int = 1): IoBuffer?
}

@Deprecated("Use readMemory instead.")
interface SuspendableReadSession : ReadSession {
    /**
     * Suspend until [atLeast] bytes become available or end of stream encountered (possibly due to exceptional close)
     *
     * @return true if there are [atLeast] bytes available or false if end of stream encountered (there still could be
     * bytes available but less than [atLeast])
     * @throws Throwable if the channel has been closed with an exception or cancelled
     * @throws IllegalArgumentException if [atLeast] is negative to too big (usually bigger that 4088)
     */
    suspend fun await(atLeast: Int = 1): Boolean
}

