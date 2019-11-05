package kotlinx.io.tests

import kotlinx.io.*
import kotlinx.io.buffer.*
import kotlin.math.*
import kotlin.test.*

class InputPrimitivesTest {

    private val bufferSizes = (1..64)
    private val fetchSizeLimit = 128
    private val prefetchSizes = (1..256)

    private fun withSequentialInput(
        limit: Int, seed: Long = 0L, body: Input.() -> Unit
    ) = prefetchSizes.forEach { prefetchSize ->
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
    fun readLong() = withSequentialInput(31) {
        assertReadLong(0x0001020304050607)
        assertReadLong(0x08090A0B0C0D0E0F)
        assertReadLong(0x1011121314151617)
        assertFalse(eof(), "EOF")
        assertFails { readLong() }
    }

    @Test
    fun readULong() = withSequentialInput(31, 0x70) {
        assertReadULong(0x7071727374757677u)
        assertReadULong(0x78797a7b7c7d7e7fu)
        assertReadULong(0x8081828384858687u)
        assertFalse(eof(), "EOF")
        assertFails { readULong() }
    }

    @Test
    fun readInt() = withSequentialInput(15) {
        assertReadInt(0x00010203)
        assertReadInt(0x04050607)
        assertReadInt(0x08090a0b)
        assertFalse(eof(), "EOF")
        assertFails { readInt() }
    }

    @Test
    fun readUInt() = withSequentialInput(15) {
        assertReadUInt(0x00010203u)
        assertReadUInt(0x04050607u)
        assertReadUInt(0x08090a0bu)
        assertFalse(eof(), "EOF")
        assertFails { readInt() }
    }

    @Test
    fun readShort() = withSequentialInput(7) {
        assertReadShort(0x0001)
        assertReadShort(0x0203)
        assertReadShort(0x0405)
        assertFalse(eof(), "EOF")
        assertFails { readShort() }
    }

    @Test
    fun readUShort() = withSequentialInput(7) {
        assertReadUShort(0x0001u)
        assertReadUShort(0x0203u)
        assertReadUShort(0x0405u)
        assertFalse(eof(), "EOF")
        assertFails { readShort() }
    }

    @Test
    fun readByte() = withSequentialInput(3) {
        assertReadByte(0x0)
        assertReadByte(0x1)
        assertReadByte(0x2)
        assertTrue(eof(), "EOF")
        assertFails { readByte() }
    }

    @Test
    fun readUByte() = withSequentialInput(3) {
        assertReadUByte(0x0u)
        assertReadUByte(0x1u)
        assertReadUByte(0x2u)
        assertTrue(eof(), "EOF")
        assertFails { readUByte() }
    }

    @Test
    fun smokeTest() {
        var closed = false
        var written = false

        val input = object : Input() {
            override fun fill(buffer: Buffer): Int {
                if (written) return 0
                written = true

                buffer.storeIntAt(0, 0x74657374) // = test
                return 4
            }

            override fun closeSource() {
                closed = true
            }
        }

        val text = input.use {
            val array = ByteArray(4)
            input.readArray(array)
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

        val input = object : Input() {
            override fun fill(buffer: Buffer): Int {
                if (items.isEmpty()) return 0
                val next = items.removeAt(0)
                for (index in 0 until next.length) {
                    buffer[index] = next[index].toByte()
                }
                return next.length
            }

            override fun closeSource() {
                items.clear()
            }
        }

        val data = ByteArray(5 + 4 + 4)
        input.readArray(data)
        @UseExperimental(ExperimentalStdlibApi::class)
        assertEquals("test.123.zxc.", data.decodeToString())
    }

}
