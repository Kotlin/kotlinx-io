package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.Test
import kotlin.test.*

open class StringsTest {
    open val pool: VerifyingObjectPool<BufferView> = VerifyingObjectPool(BufferView.NoPool)

    @AfterTest
    fun verifyPool() {
        pool.assertEmpty()
    }

    @Test
    fun testReadLineSingleBuffer() {
        val p = buildPacket {
            append("1\r22\n333\r\n4444")
        }

        assertEquals("1", p.readUTF8Line())
        assertEquals("22", p.readUTF8Line())
        assertEquals("333", p.readUTF8Line())
        assertEquals("4444", p.readUTF8Line())
        assertNull(p.readUTF8Line())
    }

    @Test
    fun testReadLineMultiBuffer() {
        val p = buildPacket {
            kotlin.repeat(1000) {
                append("1\r22\n333\r\n4444\n")
            }
        }

        repeat(1000) {
            assertEquals("1", p.readUTF8Line())
            assertEquals("22", p.readUTF8Line())
            assertEquals("333", p.readUTF8Line())
            assertEquals("4444", p.readUTF8Line())
        }

        assertNull(p.readUTF8Line())
    }

    @Test
    fun testSingleBufferReadText() {
        val p = buildPacket {
            append("ABC")
        }

        assertEquals("ABC", p.readText().toString())
    }

    @Test
    fun testMultiBufferReadText() {
        val size = 100000
        val ba = ByteArray(size) {
            'x'.toByte()
        }
        val s = CharArray(size) {
            'x'
        }.joinToString("")

        val packet = buildPacket {
            writeFully(ba)
        }

        assertEquals(s, packet.readText())
    }

    private inline fun buildPacket(startGap: Int = 0, block: BytePacketBuilder.() -> Unit): ByteReadPacket {
        val builder = BytePacketBuilder(startGap, pool)
        try {
            block(builder)
            return builder.build()
        } catch (t: Throwable) {
            builder.release()
            throw t
        }
    }
}
