package kotlinx.io.tests

import kotlinx.io.*
import kotlinx.io.buffer.*
import kotlin.random.*
import kotlin.test.*

fun sequentialInfiniteInput(fillSize: Int, bufferSize: Int = DEFAULT_BUFFER_SIZE) = object : Input(bufferSize) {
    private var value = 0L
    private var sliceRandom = Random(fillSize)

    override fun closeSource() {}

    override fun fill(buffer: Buffer): Int {
        val readLength = sliceRandom.nextInt(buffer.size) + 1
        var index = 0
        while (index < readLength) {
            buffer.storeByteAt(index++, value.toByte())
            value++
        }
        return index
    }
}

fun sequentialLimitedInput(fillSize: Int, bufferSize: Int = DEFAULT_BUFFER_SIZE, bytes: Int, seed: Long = 0L) =
    object : Input(bufferSize) {
        private var value = seed
        private var bytesLeft = bytes
        private var sliceRandom = Random(fillSize + bytes)

        override fun closeSource() {}

        override fun fill(buffer: Buffer): Int {
            // Simulate different slices being read, not just length
            val readLength = sliceRandom.nextInt(buffer.size) + 1

            if (bytesLeft == 0)
                return 0
            var index = 0
            while (index < readLength) {
                buffer.storeByteAt(index++, value.toByte())
                value++
                bytesLeft--
                if (bytesLeft == 0)
                    return index
            }
            return index
        }
    }

fun Input.assertReadLong(expected: Long) {
    val value = readLong()
    if (value == expected)
        return

    fail("Expected: ${expected.toString(16).padStart(16, '0')}, actual: ${value.toString(16).padStart(16, '0')}")
}

fun Input.assertReadULong(expected: ULong) {
    val value = readULong()
    if (value == expected)
        return

    fail("Expected: ${expected.toString(16).padStart(16, '0')}, actual: ${value.toString(16).padStart(16, '0')}")
}

fun Input.assertReadInt(expected: Int) {
    val value = readInt()
    if (value == expected)
        return

    fail("Expected: ${expected.toString(16).padStart(8, '0')}, actual: ${value.toString(16).padStart(8, '0')}")
}

fun Input.assertReadShort(expected: Short) {
    val value = readShort()
    if (value == expected)
        return

    fail("Expected: ${expected.toString(16).padStart(8, '0')}, actual: ${value.toString(16).padStart(8, '0')}")
}

private fun Long.printit(): Long {
    println(toString(16))
    return this
}
