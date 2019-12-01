package kotlinx.io.tests

import kotlinx.io.*
import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.DEFAULT_BUFFER_SIZE
import kotlinx.io.text.forEachUtf8Line
import kotlinx.io.text.readUtf8Line
import kotlinx.io.text.readUtf8Lines
import kotlin.test.*

class LimitingInputTest {

    @Test
    fun testLimit() {
        val input = StringInput("long\nlong\nline").limit(9)
        assertEquals(listOf("long", "long"), input.readUtf8Lines())
    }

    @Test
    fun testLargeLimit() {
        val input = StringInput("long\nlong\nline").limit(Long.MAX_VALUE)
        assertEquals(listOf("long", "long", "line"), input.readUtf8Lines())
    }

    @Test
    fun testLimitBufferBoundaries() {
        val size = DEFAULT_BUFFER_SIZE * 5 / 2
        val limit = DEFAULT_BUFFER_SIZE * 3 / 2
        val input = StringInput("a".repeat(size)).limit(limit)
        assertEquals("a".repeat(limit), input.readUtf8Line())
    }

    @Test
    fun testZeroLimit() {
        val input = StringInput("long\nlong\nline").limit(0)
        assertTrue(input.eof())
    }

    @Test
    fun testNegativeLimit() {
        assertFailsWith<IllegalArgumentException> { StringInput("").limit(-1) }
    }

    @Test
    fun testCloseIsDelegated() {
        var closed = false
        val input = object : Input() {
            override fun closeSource() {
                closed = true
            }

            override fun fill(buffer: Buffer): Int {
                return 0
            }
        }

        input.limit(1).forEachUtf8Line { fail() }
        assertTrue(closed)
    }

    @Test
    fun testMixedLimit() {
        buildBytes {
            writeDouble(22.1)
            repeat(20) {
                writeInt(it)
            }
        }.read {
            readDouble()
            withLimit(40) {
                val ints = IntArray(8) { readInt() }
                assertEquals(6, ints[6])
            }
            assertEquals(10,readInt())
        }
    }

    private fun StringInput(str: String) = ByteArrayInput(str.encodeToByteArray())
}
