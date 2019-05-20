package kotlinx.io.tests

import kotlin.test.*

class InputTest {
    @Test
    fun readLongsFromInfiniteInput() = (1..1024).forEach { size ->
        try {
            val input = sequentialInfiniteInput(size)
            assertEquals(0x0001020304050607, input.readLong())
            assertEquals(0x08090A0B0C0D0E0F, input.readLong())
            assertEquals(0x1011121314151617, input.readLong())
        } catch (e: Throwable) {
            println("Failed at size $size")
            throw e
        }
    }

    @Test
    fun readLongsFromLimitedBuffer() = (1..64).forEach { size ->
        try {
            val input = sequentialLimitedInput(size, 31)
            assertEquals(0x0001020304050607, input.readLong())
            assertEquals(0x08090A0B0C0D0E0F, input.readLong())
            assertEquals(0x1011121314151617, input.readLong())
            assertFails {
                input.readLong()
            }
        } catch (e: Throwable) {
            println("Failed at size $size")
            throw e
        }
    }
}

