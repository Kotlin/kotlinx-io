package kotlinx.coroutines.io

import kotlinx.coroutines.*
import kotlinx.coroutines.io.jvm.javaio.*
import kotlin.test.*

class InputAdapterTest {
    private val ch = ByteChannel(true)

    @AfterTest
    fun dispose() {
        ch.cancel()
    }

    @Test
    fun testClosedReadSingle() {
        ch.close()
        val s = ch.toInputStream()
        assertEquals(-1, s.read())
    }

    @Test
    fun testClosedReadBuffer() {
        ch.close()
        val s = ch.toInputStream()
        assertEquals(-1, s.read(ByteArray(100)))
    }

    @Test
    fun testReadSingleAfterWrite() = runBlocking {
        ch.writeStringUtf8("123")
        val s = ch.toInputStream()
        assertEquals(0x31, s.read())
        assertEquals(0x32, s.read())
        assertEquals(0x33, s.read())
    }

    @Test
    fun testReadSingleAfterWriteWithClose() = runBlocking {
        ch.writeStringUtf8("123")
        ch.close()
        val s = ch.toInputStream()
        assertEquals(0x31, s.read())
        assertEquals(0x32, s.read())
        assertEquals(0x33, s.read())
        assertEquals(-1, s.read())
    }

    @Test
    fun testReadBufferAfterWrite() = runBlocking {
        ch.writeStringUtf8("123")
        val s = ch.toInputStream()
        val array = ByteArray(3)
        assertEquals(3, s.read(array))
        assertEquals("49, 50, 51", array.joinToString(", "))
    }

    @Test
    fun testReadBufferSmallAfterWrite() = runBlocking {
        ch.writeStringUtf8("123")
        val s = ch.toInputStream()
        val array = ByteArray(2)
        assertEquals(2, s.read(array))
        assertEquals("49, 50", array.joinToString(", "))

        assertEquals(1, s.read(array))
        assertEquals(0x33, array[0])
    }

    @Test
    fun testReadBufferSmallAfterWriteWithClose() = runBlocking {
        ch.writeStringUtf8("123")
        ch.close()

        val s = ch.toInputStream()
        val array = ByteArray(2)
        assertEquals(2, s.read(array))
        assertEquals("49, 50", array.joinToString(", "))

        assertEquals(1, s.read(array))
        assertEquals(0x33, array[0])

        assertEquals(-1, s.read(array))
    }
}

