package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.test.*

class ByteArrayInputTest {

    @Test
    fun testEmptyInput() {
        testEmptyInput(ByteArray(0), 0, 0)
        testEmptyInput(ByteArray(1), 1, 1)
        testEmptyInput(ByteArray(10), 10, 10)
    }

    private fun testEmptyInput(source: ByteArray, startIndex: Int, endIndex: Int) {
        val input = ByteArrayInput(source, startIndex, endIndex)
        assertEquals(0, input.readByteArray().size)
    }

    @Test
    fun testFullInput() {
        testIndices(ByteArray(0))
        testIndices(ByteArray(42) { it.toByte() })
        testIndices(ByteArray(DEFAULT_BUFFER_SIZE * 3 + 1) { it.toByte() })
    }

    @Test
    fun testStartIndex() {
        testIndices(ByteArray(42) { it.toByte() }, startIndex = 0)
        testIndices(ByteArray(42) { it.toByte() }, startIndex = 1)
        testIndices(ByteArray(42) { it.toByte() }, startIndex = 21)
        testIndices(ByteArray(42) { it.toByte() }, startIndex = 42)

        val largeSize = DEFAULT_BUFFER_SIZE * 3 + 1
        testIndices(ByteArray(largeSize) { it.toByte() }, startIndex = 1)
        testIndices(ByteArray(largeSize) { it.toByte() }, startIndex = DEFAULT_BUFFER_SIZE)
        testIndices(ByteArray(largeSize) { it.toByte() }, startIndex = DEFAULT_BUFFER_SIZE + 2)
        testIndices(ByteArray(largeSize) { it.toByte() }, startIndex = DEFAULT_BUFFER_SIZE * 2)
    }

    @Test
    fun testEndIndex() {
        testIndices(ByteArray(42) { it.toByte() }, endIndex = 0)
        testIndices(ByteArray(42) { it.toByte() }, endIndex = 1)
        testIndices(ByteArray(42) { it.toByte() }, endIndex = 21)
        testIndices(ByteArray(42) { it.toByte() }, endIndex = 42)

        val largeSize = DEFAULT_BUFFER_SIZE * 3 + 1
        testIndices(ByteArray(largeSize) { it.toByte() }, endIndex = 1)
        testIndices(ByteArray(largeSize) { it.toByte() }, endIndex = DEFAULT_BUFFER_SIZE)
        testIndices(ByteArray(largeSize) { it.toByte() }, endIndex = DEFAULT_BUFFER_SIZE + 2)
        testIndices(ByteArray(largeSize) { it.toByte() }, endIndex = DEFAULT_BUFFER_SIZE * 2)
    }

    @Test
    fun testIndices() {
        testIndices(ByteArray(42) { it.toByte() }, startIndex = 0, endIndex = 1)
        testIndices(ByteArray(42) { it.toByte() }, startIndex = 0, endIndex = 21)
        testIndices(ByteArray(42) { it.toByte() }, startIndex = 41, endIndex = 42)

        val largeSize = DEFAULT_BUFFER_SIZE * 3 + 1
        testIndices(ByteArray(largeSize) { it.toByte() }, startIndex = 0, endIndex = 1)
        testIndices(
            ByteArray(largeSize) { it.toByte() },
            startIndex = DEFAULT_BUFFER_SIZE / 2,
            endIndex = DEFAULT_BUFFER_SIZE
        )
        testIndices(
            ByteArray(largeSize) { it.toByte() },
            startIndex = DEFAULT_BUFFER_SIZE,
            endIndex = DEFAULT_BUFFER_SIZE + 2
        )
        testIndices(
            ByteArray(largeSize) { it.toByte() },
            startIndex = DEFAULT_BUFFER_SIZE + 1,
            endIndex = DEFAULT_BUFFER_SIZE * 2 + 1
        )
    }

    private fun testIndices(array: ByteArray, startIndex: Int = 0, endIndex: Int = array.size) {
        val input = ByteArrayInput(array, startIndex, endIndex)
        val out = input.readByteArray()
        val subarray = array.slice(startIndex until endIndex).toByteArray()
        assertTrue(subarray.contentEquals(out))
    }

    @Test
    fun testIllegalIndices() {
        assertFailsWith<IllegalArgumentException> { ByteArrayInput(ByteArray(0), 0, 1) }
        assertFailsWith<IllegalArgumentException> { ByteArrayInput(ByteArray(0), 1, 1) }
        assertFailsWith<IllegalArgumentException> { ByteArrayInput(ByteArray(10), -1, 5) }
        assertFailsWith<IllegalArgumentException> { ByteArrayInput(ByteArray(10), 1, 11) }
        assertFailsWith<IllegalArgumentException> { ByteArrayInput(ByteArray(10), 2, 1) }
        assertFailsWith<IllegalArgumentException> { ByteArrayInput(ByteArray(10), -2, -1) }
    }

    @Test
    fun testString() {
        val line = "Hello, world\n"
        assertEquals(line, ByteArrayInput(line.encodeToByteArray()).readByteArray().decodeToString())
    }
}
