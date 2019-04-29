package kotlinx.io.tests

import kotlinx.io.*
import kotlin.math.*
import kotlin.test.*

class InputTest {
    private val bufferSizes = (1..64)
    private val fetchSizeLimit = 128
    private val prefetchSizes = (1..256)

    fun withInput(limit: Int, seed: Long = 0L, body: Input.() -> Unit) = prefetchSizes.forEach { prefetchSize ->
        bufferSizes.forEach { size ->
            try {
                val input = sequentialLimitedInput(fetchSizeLimit, size, limit, seed)
                assertTrue(input.prefetch(min(prefetchSize, limit)), "Can't prefetch bytes")
                input.body()
            } catch (e: Throwable) {
                println("Failed at size $size")
                throw e
            }
        }
    }

    @Test
    fun readLongs() = withInput(31) {
        assertReadLong(0x0001020304050607)
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
        assertFalse(eof(), "EOF")
        assertFails { readLong() }
    }

    @Test
    fun readULongs() = withInput(31, 0x70) {
        assertReadULong(0x7071727374757677u)
        assertReadULong(0x78797a7b7c7d7e7fu)
        assertReadULong(0x8081828384858687u)
        assertFalse(eof(), "EOF")
        assertFails { readULong() }
    }

    @Test
    fun readInts() = withInput(15) {
        assertReadInt(0x00010203)
        assertReadInt(0x04050607)
        assertReadInt(0x08090a0b)
        assertFalse(eof(), "EOF")
        assertFails { readInt() }
    }

    @Test
    fun readShorts() = withInput(7) {
        assertReadShort(0x0001)
        assertReadShort(0x0203)
        assertReadShort(0x0405)
        assertFalse(eof(), "EOF")
        assertFails { readShort() }
    }
}

