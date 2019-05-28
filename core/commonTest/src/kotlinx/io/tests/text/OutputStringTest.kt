package kotlinx.io.tests.text

import kotlinx.io.*
import kotlin.test.*

open class OutputStringTest {
    private val bufferSizes = (1..64)

    @Test
    fun testWriteAscii() = bufferSizes.forEach { size ->
        val text = "file."
        val expected = ubyteArrayOf(0x66u, 0x69u, 0x6cu, 0x65u, 0x2eu)

        val bytes = buildBytes(size) {
            writeUTF8String(text)
        }

        assertEquals(expected.size, bytes.size(), "Size $size")
        val input = bytes.input()
        val read = UByteArray(expected.size)
        input.readArray(read)
        assertEquals(expected.contentToString(), read.contentToString())
    }

    @Test
    fun testWriteUnicode() = bufferSizes.forEach { size ->
        val text = ".🌀."
        val expected = ubyteArrayOf(0x2eu, 0xf0u, 0x9fu, 0x8cu, 0x80u, 0x2eu)

        val bytes = buildBytes(size) {
            writeUTF8String(text)
        }

        assertEquals(expected.size, bytes.size(), "Size $size")
        val input = bytes.input()
        val read = UByteArray(expected.size)
        input.readArray(read)
        assertEquals(expected.contentToString(), read.contentToString())
    }


    @Test
    fun testWriteUtf8() = bufferSizes.forEach { size ->
        val text = "file content with unicode 🌀 : здороваться : 여보세요 : 你好 : ñç."
        // @formatter:off
        val expected = ubyteArrayOf(
            0x66u,0x69u,0x6cu,0x65u,0x20u,0x63u,0x6fu,0x6eu,0x74u,0x65u,0x6eu,0x74u,0x20u,
            0x77u,0x69u,0x74u,0x68u,0x20u,0x75u,0x6eu,0x69u,0x63u,0x6fu,0x64u,0x65u,0x20u, // ascii ends
            0xf0u,0x9fu,0x8cu,0x80u,0x20u,0x3au,0x20u,0xd0u,0xb7u,0xd0u,0xb4u,0xd0u,0xbeu,0xd1u,0x80u,0xd0u,0xbeu,
            0xd0u,0xb2u,0xd0u,0xb0u,0xd1u,0x82u,0xd1u,0x8cu,0xd1u,0x81u,0xd1u,0x8fu,0x20u,0x3au,0x20u,0xecu,
            0x97u,0xacu,0xebu,0xb3u,0xb4u,0xecu,0x84u,0xb8u,0xecu,0x9au,0x94u,0x20u,0x3au,0x20u,0xe4u,0xbdu,0xa0u,
            0xe5u,0xa5u,0xbdu,0x20u,0x3au,0x20u,0xc3u,0xb1u,0xc3u,0xa7u,0x2eu)
        // @formatter:on

        val bytes = buildBytes(size) {
            writeUTF8String(text)
        }

        assertEquals(expected.size, bytes.size(), "Size $size")

        val input = bytes.input()
        val read = UByteArray(expected.size)
        input.readArray(read)
        assertEquals(expected.contentToString(), read.contentToString())
    }
    
    @Test
    fun testWriteMultiByteAtEnd() {
        val input = buildBytes {
            writeUTF8String("ABC\u0422")
        }.input()

        assertEquals("ABC\u0422", input.readUTF8String(4))
    }

    @Test
    fun testWriteSingleByte() {
        val input = buildBytes {
            writeUTF8String("1")
        }.input()

        try {
            assertEquals("1", input.readUTF8String(1))
        } finally {
            input.close()
        }
    }

    @Test
    fun testReadUntilDelimiter() {
        val input = buildBytes {
            writeUTF8String("1,23|,4.")
        }.input()

        val sb = StringBuilder()
        val counts = mutableListOf<Int>()

        counts.add(input.readUTF8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUTF8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUTF8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUTF8StringUntilDelimitersTo(sb, "|,."))
        assertEquals("1234", sb.toString())
        assertEquals(listOf(1, 2, 0, 1), counts)
    }
    
    @Test
    fun testReadUntilDelimiterMulti() {
        val input = buildBytes {
            writeUTF8String("\u0422,\u0423|\u0424.")
        }.input()

        val sb = StringBuilder()
        val counts = mutableListOf<Int>()

        counts.add(input.readUTF8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUTF8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUTF8StringUntilDelimitersTo(sb, "|,."))
        assertEquals("\u0422\u0423\u0424", sb.toString())
        assertEquals(listOf(1, 1, 1), counts)
    }

    @Test
    fun testReadLineSingleBuffer() = bufferSizes.forEach { size ->
        val input = buildBytes(size) {
            writeUTF8String("1\r\n22\n333\n4444\n") // TODO: replace one LF with CR when we can read it 
        }.input()

        assertEquals("1", input.readUTF8Line())
        assertEquals("22", input.readUTF8Line())
        assertEquals("333", input.readUTF8Line())
        assertEquals("4444", input.readUTF8Line())
        assertFails { input.readUTF8Line() }
    }
}

