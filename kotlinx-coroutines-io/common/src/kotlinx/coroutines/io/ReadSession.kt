package kotlinx.coroutines.io

import kotlinx.io.core.*

@ExperimentalIoApi
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

@ExperimentalIoApi
interface SuspendableReadSession : ReadSession {
    /**
     * Suspend until [atLeast] bytes become available or end of stream encountered (possibly due to exceptional close)
     *
     * @return true if there are [atLeast] bytes available or false if end of stream encountered (there still could be
     * bytes available but less than [atLeast])
     * @throws Throwable if the channel has been closed with an exception or cancelled
     */
    suspend fun await(atLeast: Int = 1): Boolean
}

