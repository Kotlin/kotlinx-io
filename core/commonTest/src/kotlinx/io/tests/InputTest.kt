package kotlinx.io.tests

import kotlin.test.*

class InputTest {
    @Test
    fun `Read 3 longs from infinite Input with buffer sizes from 1 to 1024`() = (1..1024).forEach { size ->
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
    fun `Read longs from limited 24 bytes Input with buffer sizes from 1 to 64`() = (1..64).forEach { size ->
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

