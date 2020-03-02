package kotlinx.io.text

import kotlinx.io.*
import kotlinx.io.bytes.*
import kotlin.test.*

class OutputStringTest {
    private val bufferSizes = (1..64)

    @Test
    fun testWriteAscii() = bufferSizes.forEach { size ->
        val text = "file."
        val expected = ubyteArrayOf(0x66u, 0x69u, 0x6cu, 0x65u, 0x2eu)

        val input = buildInput(size) {
            writeUtf8String(text)
        }

        assertEquals(expected.size, input.remaining, "Size $size")
        val read = UByteArray(expected.size)
        input.readByteArray(read)
        assertTrue(input.exhausted(), "EOF")
        assertEquals(expected.contentToString(), read.contentToString())
    }

    @Test
    fun testWriteUnicode() = bufferSizes.forEach { size ->
        val text = ".🌀."
        val expected = ubyteArrayOf(0x2eu, 0xf0u, 0x9fu, 0x8cu, 0x80u, 0x2eu)

        val input = buildInput(size) {
            writeUtf8String(text)
        }

        assertEquals(expected.size, input.remaining, "Size $size")
        val read = UByteArray(expected.size)
        input.readByteArray(read)
        assertTrue(input.exhausted(), "EOF")
        assertEquals(expected.contentToString(), read.contentToString())
    }

    @Test
    fun testWriteUtf8() = bufferSizes.forEach { size ->
        val text = "file content with unicode 🌀 : здороваться : 여보세요 : 你好 : ñç."
        // @formatter:off
        val expected = ubyteArrayOf(
            0x66u, 0x69u, 0x6cu, 0x65u, 0x20u, 0x63u, 0x6fu, 0x6eu, 0x74u, 0x65u, 0x6eu, 0x74u, 0x20u,
            0x77u, 0x69u, 0x74u, 0x68u, 0x20u, 0x75u, 0x6eu, 0x69u, 0x63u, 0x6fu, 0x64u, 0x65u, 0x20u, // ascii ends
            0xf0u, 0x9fu, 0x8cu, 0x80u, 0x20u, 0x3au, 0x20u, 0xd0u, 0xb7u, 0xd0u, 0xb4u, 0xd0u, 0xbeu,
            0xd1u, 0x80u, 0xd0u, 0xbeu, 0xd0u, 0xb2u, 0xd0u, 0xb0u, 0xd1u, 0x82u, 0xd1u, 0x8cu, 0xd1u,
            0x81u, 0xd1u, 0x8fu, 0x20u, 0x3au, 0x20u, 0xecu, 0x97u, 0xacu, 0xebu, 0xb3u, 0xb4u, 0xecu,
            0x84u, 0xb8u, 0xecu, 0x9au, 0x94u, 0x20u, 0x3au, 0x20u, 0xe4u, 0xbdu, 0xa0u,
            0xe5u, 0xa5u, 0xbdu, 0x20u, 0x3au, 0x20u, 0xc3u, 0xb1u, 0xc3u, 0xa7u, 0x2eu
        )
        // @formatter:on

        val input = buildInput(size) {
            writeUtf8String(text)
        }

        assertEquals(expected.size, input.remaining, "Size $size")

        val read = UByteArray(expected.size)
        input.readByteArray(read)
        assertTrue(input.exhausted(), "EOF")
        assertEquals(expected.contentToString(), read.contentToString())
    }

    @Test
    fun testWriteUtf8Chars() = bufferSizes.forEach { size ->
        val text = "file content with unicode  : здороваться : 여보세요 : 你好 : ñç."
        // @formatter:off
        val expected = ubyteArrayOf(
            0x66u, 0x69u, 0x6cu, 0x65u, 0x20u, 0x63u, 0x6fu, 0x6eu, 0x74u, 0x65u, 0x6eu, 0x74u, 0x20u,
            0x77u, 0x69u, 0x74u, 0x68u, 0x20u, 0x75u, 0x6eu, 0x69u, 0x63u, 0x6fu, 0x64u, 0x65u, 0x20u, // ascii ends
            0x20u, 0x3au, 0x20u, 0xd0u, 0xb7u, 0xd0u, 0xb4u, 0xd0u, 0xbeu,
            0xd1u, 0x80u, 0xd0u, 0xbeu, 0xd0u, 0xb2u, 0xd0u, 0xb0u, 0xd1u, 0x82u, 0xd1u, 0x8cu, 0xd1u,
            0x81u, 0xd1u, 0x8fu, 0x20u, 0x3au, 0x20u, 0xecu, 0x97u, 0xacu, 0xebu, 0xb3u, 0xb4u, 0xecu,
            0x84u, 0xb8u, 0xecu, 0x9au, 0x94u, 0x20u, 0x3au, 0x20u, 0xe4u, 0xbdu, 0xa0u,
            0xe5u, 0xa5u, 0xbdu, 0x20u, 0x3au, 0x20u, 0xc3u, 0xb1u, 0xc3u, 0xa7u, 0x2eu
        )
        // @formatter:on

        val input = buildInput(size) {
            text.forEach { writeUtf8Char(it) }
        }

        assertEquals(expected.size, input.remaining, "Size $size")

        val read = UByteArray(expected.size)
        input.readByteArray(read)
        assertTrue(input.exhausted(), "EOF")
        assertEquals(expected.contentToString(), read.contentToString())
    }

    @Test
    fun testWriteMultiByteAtEnd() {
        val input = buildInput {
            writeUtf8String("ABC\u0422")
        }

        assertEquals("ABC\u0422", input.readUtf8String(4))
        assertTrue(input.exhausted(), "EOF")
    }

    @Test
    fun testWriteMultiByteAtEndParts() {
        val input = buildBytes {
            writeUtf8String("ABC\u0422")
        }.input()

        assertEquals("ABC", input.readUtf8String(3))
        assertEquals("\u0422", input.readUtf8String(1))
        assertTrue(input.exhausted(), "EOF")
    }

    @Test
    fun testWriteSingleByte() {
        val input = buildInput {
            writeUtf8String("1")
        }

        assertEquals("1", input.readUtf8String(1))
        assertTrue(input.exhausted(), "EOF")
    }

    @Test
    fun testReadUntilDelimiter() {
        val input = buildInput {
            writeUtf8String("1,23|,4.")
        }

        val sb = StringBuilder()
        val counts = mutableListOf<Int>()

        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        assertTrue(input.exhausted(), "EOF")
        assertEquals("1234", sb.toString())
        assertEquals(listOf(1, 2, 0, 1), counts)
    }

    @Test
    fun testReadUntilDelimiterUnicode() {
        // 134 139 195
        val input = buildBytes {
            writeUtf8String("1,23|,4.🤔.1.\u0422")
        }.input()

        val sb = StringBuilder()
        val counts = mutableListOf<Int>()

        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        counts.add(input.readUtf8StringUntilDelimitersTo(sb, "|,."))
        assertTrue(input.exhausted(), "EOF")
        assertEquals("1234🤔1\u0422", sb.toString())
        assertEquals(listOf(1, 2, 0, 1, 2, 1, 1), counts)
    }

    @Test
    fun testReadUntilDelimiterMulti() {
        val input = buildInput {
            writeUtf8String("\u0422,\u0423|\u0424.")
        }

        assertEquals(9, input.remaining)
        val builder = StringBuilder()
        val counts = mutableListOf<Int>()

        counts.add(input.readUtf8StringUntilDelimitersTo(builder, "|,."))
        assertEquals(6, input.remaining)
        counts.add(input.readUtf8StringUntilDelimitersTo(builder, "|,."))
        assertEquals(3, input.remaining)
        counts.add(input.readUtf8StringUntilDelimitersTo(builder, "|,."))
        assertEquals(0, input.remaining)
        assertEquals("\u0422\u0423\u0424", builder.toString())
        assertTrue(input.exhausted(), "EOF")
        assertEquals(listOf(1, 1, 1), counts)
    }

    @Test
    fun testReadLineSingleBuffer() = bufferSizes.forEach { size ->
        val input = buildInput(size) {
            writeUtf8String("1\r\n22\n333\n4444\n") // TODO: replace one LF with CR when we can read it
        }

        assertEquals("1", input.readUtf8Line())
        assertEquals("22", input.readUtf8Line())
        assertEquals("333", input.readUtf8Line())
        assertEquals("4444", input.readUtf8Line())
        assertTrue(input.exhausted(), "EOF")
    }

    @Test
    fun testWriteSingleUnicode() {
        val text = """🤔"""
        val input = buildInput {
            writeUtf8String(text, 0, text.length)
        }

        assertEquals(text, input.readUtf8String())
    }

    @Test
    fun testParseGlyph() {
        val text = """􏿿"""
        val input = buildInput {
            writeUtf8String(text, 0, text.length)
        }

        assertEquals(text, input.readUtf8String())
    }
}

