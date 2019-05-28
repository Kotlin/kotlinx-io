package kotlinx.io.tests

import kotlinx.io.*
import kotlin.test.*

class BytesTest {
    private val bufferSizes = (1..64)

    @Test
    fun smokeSingleBufferTest() = bufferSizes.forEach { size ->
        val bytes = buildBytes(size) {
            val ba = ByteArray(2)
            ba[0] = 0x11
            ba[1] = 0x22
            writeArray(ba)

            writeByte(0x12)
            writeUByte(0x82u)
            writeShort(0x3456)
            writeInt(0x789abcde)
            writeDouble(1.25)
            writeFloat(1.25f)
            writeLong(0x123456789abcdef0)
            writeLong(0x123456789abcdef0)

            writeUTF8String("OK\n")
            //listOf(1, 2, 3).joinTo(this, separator = "|")
        }

        assertEquals(2 + 2 + 2 + 4 + 8 + 4 + 8 + 8 + 3/* + 5*/, bytes.size())

        val input = bytes.asInput()
        val ba = ByteArray(2)
        input.readArray(ba)

        assertEquals(0x11, ba[0])
        assertEquals(0x22, ba[1])

        assertEquals(0x12, input.readByte())
        assertEquals(0x82u, input.readUByte())
        assertEquals(0x3456, input.readShort())
        assertEquals(0x789abcde, input.readInt())
        assertEquals(1.25, input.readDouble())
        assertEquals(1.25f, input.readFloat())

        val ll = (1..8).map { input.readByte().toInt() and 0xff }.joinToString()
        assertEquals("18, 52, 86, 120, 154, 188, 222, 240", ll)
        assertEquals(0x123456789abcdef0, input.readLong())

        assertEquals("OK", input.readUTF8Line())
/*
        assertEquals("1|2|3", input.readUTF8Line())
*/

        //assertTrue { bytes.isEmpty }
    }
}