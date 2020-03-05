package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.text.*
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
        assertTrue(input.exhausted())
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

            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                return 0
            }
        }

        input.limit(1).forEachUtf8Line { fail() }
        assertTrue(closed)
    }

}
