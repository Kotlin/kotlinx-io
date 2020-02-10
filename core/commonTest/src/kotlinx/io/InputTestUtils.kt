package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.random.*
import kotlin.test.*


fun sequentialInfiniteInput(
    fillSize: Int, bufferSize: Int = DEFAULT_BUFFER_SIZE
): Input = object : Input(DefaultBufferPool(bufferSize)) {
    private var value = 0L
    private var sliceRandom = Random(fillSize)

    override fun closeSource() {}

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        val readLength = sliceRandom.nextInt(buffer.size) + 1
        var index = 0
        while (index < readLength) {
            buffer.storeByteAt(startIndex + index++, value.toByte())
            value++
        }
        return index
    }
}

fun sequentialLimitedInput(
    fillSize: Int, bufferSize: Int = DEFAULT_BUFFER_SIZE, bytes: Int, seed: Long = 0L
): Input = object : Input(DefaultBufferPool(bufferSize)) {
    private var value = seed
    private var bytesLeft = bytes
    private var sliceRandom = Random(fillSize + bytes)

    override fun closeSource() {}

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        // Simulate different slices being read, not just length
        val readLength = sliceRandom.nextInt(buffer.size) + 1
        if (bytesLeft == 0) return 0
        var index = 0
        while (index < readLength) {
            buffer.storeByteAt(startIndex + index++, value.toByte())
            value++
            bytesLeft--
            if (bytesLeft == 0)
                return index
        }
        return index
    }
}

class LambdaInput(private val block: (buffer: Buffer, startIndex: Int, endIndex: Int) -> Int) : Input() {
    override fun closeSource() {}
    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int = block(buffer, startIndex, endIndex)
}

fun Input.assertReadLong(expected: Long) {
    val value = readLong()
    if (value == expected) return
    fail("Expected: ${expected.toString(16).padStart(16, '0')}, actual: ${value.toString(16).padStart(16, '0')}")
}

fun Input.assertReadULong(expected: ULong) {
    val value = readULong()
    if (value == expected) return
    fail("Expected: ${expected.toString(16).padStart(16, '0')}, actual: ${value.toString(16).padStart(16, '0')}")
}

fun Input.assertReadInt(expected: Int) {
    val value = readInt()
    if (value == expected) return
    fail("Expected: ${expected.toString(16).padStart(8, '0')}, actual: ${value.toString(16).padStart(8, '0')}")
}


fun Input.assertReadUInt(expected: UInt) {
    val value = readUInt()
    if (value == expected) return
    fail("Expected: ${expected.toString(16).padStart(8, '0')}, actual: ${value.toString(16).padStart(8, '0')}")
}

fun Input.assertReadShort(expected: Short) {
    val value = readShort()
    if (value == expected) return
    fail("Expected: ${expected.toString(16).padStart(8, '0')}, actual: ${value.toString(16).padStart(8, '0')}")
}

fun Input.assertReadUShort(expected: UShort) {
    val value = readUShort()
    if (value == expected) return
    fail("Expected: ${expected.toString(16).padStart(8, '0')}, actual: ${value.toString(16).padStart(8, '0')}")
}

fun Input.assertReadByte(expected: Byte) {
    val value = readByte()
    if (value == expected) return
    fail("Expected: ${expected.toString(16).padStart(8, '0')}, actual: ${value.toString(16).padStart(8, '0')}")
}

fun Input.assertReadUByte(expected: UByte) {
    val value = readUByte()
    if (value == expected) return
    fail("Expected: ${expected.toString(16).padStart(8, '0')}, actual: ${value.toString(16).padStart(8, '0')}")
}

