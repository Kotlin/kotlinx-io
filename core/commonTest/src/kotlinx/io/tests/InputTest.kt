package kotlinx.io.tests

import kotlinx.io.*
import kotlin.test.*

class InputTest {
    private val bufferSizes = (1..64)
    private val fetchSizeLimit = 128

    fun withInput(limit: Int, seed: Long = 0L, body: Input.() -> Unit) = bufferSizes.forEach { size ->
        try {
            sequentialLimitedInput(fetchSizeLimit, size, limit, seed).apply(body)
        } catch (e: Throwable) {
            println("Failed at size $size")
            throw e
        }
    }

    @Test
    fun readLongs() = withInput(31) {
        assertReadLong(0x0001020304050607)
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
        assertFails { readLong() }
    }

    @Test
    fun readULongs() = withInput(31, 0x70) {
        assertReadULong(0x7071727374757677u)
        assertReadULong(0x78797a7b7c7d7e7fu)
        assertReadULong(0x8081828384858687u)
        assertFails { readULong() }
    }

    @Test
    fun readInts() = withInput(15) {
        assertReadInt(0x00010203)
        assertReadInt(0x04050607)
        assertReadInt(0x08090a0b)
        assertFails { readInt() }
    }

    @Test
    fun readShorts() = withInput(7) {
        assertReadShort(0x0001)
        assertReadShort(0x0203)
        assertReadShort(0x0405)
        assertFails { readShort() }
    }
}

