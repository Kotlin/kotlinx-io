package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.math.*
import kotlin.test.*

class InputPrimitivesTest : LeakDetector() {

    private val bufferSizes = (1..64)
    private val fetchSizeLimit = 128
    private val prefetchSizes = (1..256)

    private fun withSequentialInput(
        limit: Int, seed: Long = 0L, body: Input.() -> Unit
    ) = prefetchSizes.forEach { prefetchSize ->
        bufferSizes.forEach { size ->
            val input = sequentialLimitedInput(fetchSizeLimit, pool, limit, seed)
            try {
                assertTrue(input.prefetch(min(prefetchSize, limit)), "Can't prefetch bytes")
                input.body()
            } catch (e: Throwable) {
                println("Failed at size $size")
                throw e
            } finally {
                input.close()
            }
        }
    }

    @Test
    fun testReadLong() = withSequentialInput(31) {
        assertReadLong(0x0001020304050607)
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
        assertFalse(exhausted(), "EOF")
        assertFails { readLong() }
    }

    @Test
    fun testReadULong() = withSequentialInput(31, 0x70) {
        assertReadULong(0x7071727374757677u)
        assertReadULong(0x78797a7b7c7d7e7fu)
        assertReadULong(0x8081828384858687u)
        assertFalse(exhausted(), "EOF")
        assertFails { readULong() }
    }

    @Test
    fun testReadInt() = withSequentialInput(15) {
        assertReadInt(0x00010203)
        assertReadInt(0x04050607)
        assertReadInt(0x08090a0b)
        assertFalse(exhausted(), "EOF")
        assertFails { readInt() }
    }

    @Test
    fun testReadUInt() = withSequentialInput(15) {
        assertReadUInt(0x00010203u)
        assertReadUInt(0x04050607u)
        assertReadUInt(0x08090a0bu)
        assertFalse(exhausted(), "EOF")
        assertFails { readInt() }
    }

    @Test
    fun testReadShort() = withSequentialInput(7) {
        assertReadShort(0x0001)
        assertReadShort(0x0203)
        assertReadShort(0x0405)
        assertFalse(exhausted(), "EOF")
        assertFails { readShort() }
    }

    @Test
    fun testReadUShort() = withSequentialInput(7) {
        assertReadUShort(0x0001u)
        assertReadUShort(0x0203u)
        assertReadUShort(0x0405u)
        assertFalse(exhausted(), "EOF")
        assertFails { readShort() }
    }

    @Test
    fun testReadByte() = withSequentialInput(3) {
        assertReadByte(0x0)
        assertReadByte(0x1)
        assertReadByte(0x2)
        assertTrue(exhausted(), "EOF")
        assertFails { readByte() }
    }

    @Test
    fun testReadUByte() = withSequentialInput(3) {
        assertReadUByte(0x0u)
        assertReadUByte(0x1u)
        assertReadUByte(0x2u)
        assertTrue(exhausted(), "EOF")
        assertFails { readUByte() }
    }

    @Test
    fun testSmoke() {
        var closed = false
        var written = false

        class MyInput : Input() {
            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                if (written) return 0
                written = true

                buffer.storeIntAt(startIndex, 0x74657374) // = test
                return 4
            }

            override fun closeSource() {
                closed = true
            }
        }

        val input = MyInput()
        val text = input.use {
            val array = ByteArray(4)
            input.readByteArray(array)
            array
        }

        assertEquals(true, closed, "Should be closed")

        @UseExperimental(ExperimentalStdlibApi::class)
        assertEquals("test", text.decodeToString(), "Content read")
    }

    @Test
    fun testCopy() {
        val items = arrayListOf(
            "test.", "123.", "zxc."
        )

        class MyInput : Input() {
            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                if (items.isEmpty()) return 0
                val next = items.removeAt(0)
                for (index in 0 until next.length) {
                    buffer[startIndex + index] = next[index].toByte()
                }
                return next.length
            }

            override fun closeSource() {
                items.clear()
            }
        }

        val input = MyInput()

        val data = ByteArray(5 + 4 + 4)
        input.readByteArray(data)
        @UseExperimental(ExperimentalStdlibApi::class)
        assertEquals("test.123.zxc.", data.decodeToString())
    }

}
