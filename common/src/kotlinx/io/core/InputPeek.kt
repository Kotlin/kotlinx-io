package kotlinx.io.core

/**
 * Copy at least [min] but up to [max] bytes to the specified [destination] buffer from this input
 * skipping [offset] bytes. If there are not enough bytes available to provide [min] bytes then
 * it simply return number of available bytes with no exception so the returned value need
 * to be checked.
 * It is safe to specify `max > destination.writeRemaining` but
 * `min` shouldn't be bigger than the [destination] free space.
 * This function could trigger the underlying source reading that may lead to blocking I/O.
 * It is safe to specify too big [offset] so in this case this function will always return `0`.
 * This function usually copy more bytes than [min] (unless `max = min`) but it is not guaranteed.
 * When `0` is returned with `offset = 0` then it makes sense to check [Input.endOfInput].
 *
 * @param destination to write bytes
 * @param offset to skip input
 * @param min bytes to be copied, shouldn't be greater than the buffer free space. Could be `0`.
 * @param max bytes to be copied even if there are more bytes buffered, could be [Int.MAX_VALUE].
 * @return number of bytes copied to the [destination] possibly `0`
 */
@ExperimentalIoApi
fun Input.peekTo(destination: IoBuffer, offset: Int = 0, min: Int = 1, max: Int = Int.MAX_VALUE): Int {
    checkPeekTo(destination, offset, min, max)

    if (this is IoBuffer) {
        return peekToImpl(destination, offset, min, max)
    }
    if (this is ByteReadPacketBase) {
        prefetch(offset + min)
        return peekToImpl(destination, offset, min, max)
    }

    throw UnsupportedOperationException("This only works for builtin Inputs and AbstractInput implementations")
}

private fun IoBuffer.peekToImpl(destination: IoBuffer, offset: Int, min: Int, max: Int): Int {
    val readRemaining = readRemaining
    if (readRemaining == 0 || offset > readRemaining) return 0

    val size = minOf(readRemaining - offset, destination.writeRemaining, max)
    discardExact(offset)
    writeFully(this, size)
    pushBack(size + offset)

    return size
}

private fun checkPeekTo(destination: IoBuffer, offset: Int, min: Int, max: Int) {
    kotlinx.io.core.internal.require(offset >= 0) { "offset shouldn't be negative: $offset." }
    kotlinx.io.core.internal.require(min >= 0) { "min shouldn't be negative: $min." }
    kotlinx.io.core.internal.require(max >= min) { "max should't be less than min: max = $max, min = $min." }
    kotlinx.io.core.internal.require(min <= destination.writeRemaining) {
        "Not enough free space in the destination buffer " +
            "to write the specified minimum number of bytes: min = $min, free = ${destination.writeRemaining}."
    }
}
