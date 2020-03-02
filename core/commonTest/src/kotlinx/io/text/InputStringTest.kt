package kotlinx.io.text

import kotlinx.io.*
import kotlinx.io.bytes.*
import kotlin.test.*

class InputStringTest {
    private val bufferSizes = (1..64)

    val expected = "file content with unicode 🌀 : здороваться : 여보세요 : 你好 : ñç."

    private val content = ubyteArrayOf(
        0x66u, 0x69u, 0x6cu, 0x65u, 0x20u, 0x63u, 0x6fu, 0x6eu, 0x74u, 0x65u, 0x6eu, 0x74u, 0x20u,
        0x77u, 0x69u, 0x74u, 0x68u, 0x20u, 0x75u, 0x6eu, 0x69u, 0x63u, 0x6fu, 0x64u, 0x65u, 0x20u, 0xf0u, 0x9fu,
        0x8cu, 0x80u, 0x20u, 0x3au, 0x20u, 0xd0u, 0xb7u, 0xd0u, 0xb4u, 0xd0u, 0xbeu, 0xd1u, 0x80u, 0xd0u, 0xbeu,
        0xd0u, 0xb2u, 0xd0u, 0xb0u, 0xd1u, 0x82u, 0xd1u, 0x8cu, 0xd1u, 0x81u, 0xd1u, 0x8fu, 0x20u, 0x3au, 0x20u, 0xecu,
        0x97u, 0xacu, 0xebu, 0xb3u, 0xb4u, 0xecu, 0x84u, 0xb8u, 0xecu, 0x9au, 0x94u, 0x20u, 0x3au, 0x20u, 0xe4u, 0xbdu,
        0xa0u, 0xe5u, 0xa5u, 0xbdu, 0x20u, 0x3au, 0x20u, 0xc3u, 0xb1u, 0xc3u, 0xa7u, 0x2eu
    )

    @Test
    fun testDecodeUtf8FromInputUntil() = bufferSizes.forEach { size ->
        val input = buildInput(size) {
            writeByteArray(content)
        }
        val result = input.readUtf8StringUntilDelimiter('.')
        assertEquals(expected.dropLast(1), result)
    }

    @Test
    fun testDecodeUtf8FromInput() = bufferSizes.forEach { size ->
        val input = buildInput(size) {
            writeByteArray(content)
        }
        val result = input.readUtf8String(expected.length)
        assertEquals(expected, result)
    }

    @Test
    fun testDecodeUtf8FromInputWithoutLength() = bufferSizes.forEach { size ->
        val input = buildInput(size) { writeByteArray(content) }
        val result = input.readUtf8String()
        assertEquals(expected, result)
    }
}
