package kotlinx.io.tests

import kotlinx.io.*
import kotlinx.io.memory.*
import kotlin.random.*

fun sequentialInfiniteInput(fillSize: Int, pageSize: Int = DEFAULT_PAGE_SIZE) = object : Input(pageSize) {
    private var value = 0L
    private var sliceRandom = Random(fillSize)

    override fun close() {}

    override fun fill(destination: Memory, offset: Int, length: Int): Int {
        // Simulate different slices being read, not just length
        val readLength = sliceRandom.nextInt(length) + 1

        var index = offset
        while (index < offset + readLength) {
            destination.storeAt(index++, value.toByte())
            value++
        }
        return index - offset
    }
}

fun sequentialLimitedInput(fillSize: Int, bytes: Int, pageSize: Int = DEFAULT_PAGE_SIZE) = object : Input(pageSize) {
    private var value = 0L
    private var bytesLeft = bytes
    private var sliceRandom = Random(fillSize + bytes)

    override fun close() {}

    override fun fill(destination: Memory, offset: Int, length: Int): Int {
        // Simulate different slices being read, not just length
        val readLength = sliceRandom.nextInt(length) + 1

        if (bytesLeft == 0)
            return 0
        var index = offset
        while (index < offset + readLength) {
            destination.storeAt(index++, value.toByte())
            value++
            bytesLeft--
            if (bytesLeft == 0)
                return index - offset
        }
        return index - offset
    }
}