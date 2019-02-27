package kotlinx.io.tests

import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import kotlin.test.*

class AbstractInputTest {
    @Test
    fun smokeTest() {
        var closed = false

        val input = object : AbstractInput() {
            override fun fill(destination: Buffer): Boolean {
                destination.append("test")
                return true
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
        val items = arrayListOf(
            "test.", "123.", "zxc."
        )

        val input = object : AbstractInput() {
            override fun fill(): ChunkBuffer? {
                if (items.isEmpty()) return null
                return super.fill()
            }

            override fun fill(destination: Buffer): Boolean {
                val next = items.removeAt(0)
                destination.append(next)
                return items.isEmpty()
            }

            override fun closeSource() {
                items.clear()
            }
        }

        val out = BytePacketBuilder()
        input.copyTo(out)
        assertEquals("test.123.zxc.", out.build().readText())
    }
}
