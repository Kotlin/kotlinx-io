package kotlinx.io.tests

import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import kotlin.test.*

class AbstractOutputTest {
    @Test
    fun smokeTest() {
        val builder = BytePacketBuilder()

        val output = object : AbstractOutput() {
            override fun closeDestination() {
            }

            override fun flush(buffer: Buffer) {
                builder.writeFully(buffer)
            }
        }

        output.use {
            it.append("test")
        }

        val pkt = builder.build().readText()
        assertEquals("test", pkt)
    }

    @Test
    fun testCopy() {
        val result = BytePacketBuilder()

        val output = object : AbstractOutput() {
            override fun closeDestination() {
            }

            override fun flush(buffer: Buffer) {
                result.writeFully(buffer)
            }
        }

        val fromHead = ChunkBuffer.Pool.borrow()
        var current = fromHead
        repeat(3) {
            current.append("test $it. ")
            val next = ChunkBuffer.Pool.borrow()
            current.next = next
            current = next
        }

        current.append("end.")

        val from = ByteReadPacket(fromHead, ChunkBuffer.Pool)

        from.copyTo(output)
        output.flush()

        assertEquals("test 0. test 1. test 2. end.", result.build().readText())
    }
}
