package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.*

class AbstractInputTest {
    @Test
    fun smokeTest() {
        var closed = false
        var chunk: IoBuffer? = IoBuffer.Pool.borrow().apply { append("test") }

        val input = object : AbstractInput() {
            override fun fill(): IoBuffer? {
                val next = chunk
                chunk = null
                return next
            }

            override fun closeSource() {
                closed = true
            }
        }

        val text = input.use {
            input.readBytes()
        }

        assertEquals(true, closed, "Should be closed")
        assertEquals("test", String(text), "Content read")
    }

    @Test
    fun testCopy() {
        val items = ArrayList<IoBuffer>().apply {
            add(IoBuffer.Pool.borrow().apply { append("test.") })
            add(IoBuffer.Pool.borrow().apply { append("123.") })
            add(IoBuffer.Pool.borrow().apply { append("zxc.") })
        }

        val input = object : AbstractInput() {
            override fun fill(): IoBuffer? {
                if (items.isEmpty()) return null

                return items.removeAt(0)
            }

            override fun closeSource() {
                items.forEach { it.release(IoBuffer.Pool) }
                items.clear()
            }
        }

        val out = BytePacketBuilder()
        input.copyTo(out)
        assertEquals("test.123.zxc.", out.build().readText())
    }
}
