package kotlinx.io

/**
 * Creates an input from the given byte array, starting from inclusively [startIndex] and until [endIndex] exclusively.
 * The array is not copied and calling [close][Input.close] on the resulting input has no effect.
 */
public fun ByteArrayInput(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): Input {
    require(startIndex in 0..endIndex && endIndex <= source.size) {
        "Invalid range of indices ($startIndex..$endIndex) for array of size ${source.size}"
    }
    return kotlinx.io.internal.ByteArrayInput(source, startIndex, endIndex)
}
