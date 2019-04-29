package kotlinx.io.tests

import kotlinx.io.*
import kotlinx.io.memory.*
import kotlin.random.*
import kotlin.test.*

class InputTest {
    private fun sequentialInfiniteInput(pageSize: Int): Input {
        return object : Input() {
            private var value = 0L
            private var sliceRandom = Random(pageSize)

            override fun allocatePage(): Memory = Memory.allocate(pageSize)
            override fun releasePage(memory: Memory) = Memory.release(memory)
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
    }

    private fun sequentialLimitedInput(pageSize: Int, bytes: Int): Input {
        return object : Input() {
            private var value = 0L
            private var bytesLeft = bytes
            private var sliceRandom = Random(pageSize + bytes)

            override fun allocatePage(): Memory = Memory.allocate(pageSize)
            override fun releasePage(memory: Memory) = Memory.release(memory)
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
    }

    @Test
    fun `Read 3 longs from infinite Input with page sizes from 1 to 1024`() = (1..1024).forEach { size ->
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
    fun `Read longs from limited 24 bytes Input with page sizes from 1 to 64`() = (1..64).forEach { size ->
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