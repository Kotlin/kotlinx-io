package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.text.*
import java.io.*
import kotlin.io.DEFAULT_BUFFER_SIZE
import kotlin.random.*
import kotlin.test.*

class InputStreamTest {
    // I as IS
    @Test
    fun testInputAsInputStream() {
        val input = StringInput("1\n2")
        val lines = input.asInputStream().bufferedReader().readLines()
        assertEquals(listOf("1", "2"), lines)
    }

    @Test
    fun testInputAsInputStreamBufferBoundary() {
        val baseline = "1".repeat(DEFAULT_BUFFER_SIZE + 1) + "\n" + "2".repeat(DEFAULT_BUFFER_SIZE + 1)
        val input = StringInput(baseline)
        val lines = input.asInputStream().bufferedReader().readLines()
        assertEquals(baseline.split("\n"), lines)
    }

    @Test
    fun testInputAsInputStreamNegativeValues() {
        val baseline = byteArrayOf(1, -1, -127, 0, -125, 127, -42, -1, 3)
        val input = ByteArrayInput(baseline)
        val stream = input.asInputStream()
        val result = ByteArray(baseline.size)
        stream.read(result)
        assertEquals(-1, stream.read())
        assertArrayEquals(baseline, result)
    }

    @Test
    fun testEmptyInputAsInputStream() {
        val input = ByteArrayInput(byteArrayOf()).asInputStream()
        assertEquals(-1, input.read())
    }

    @Test
    fun testInputAsInputStreamClose() {
        var closed = false
        val input = object : Input() {
            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                return 0
            }

            override fun closeSource() {
                closed = true
            }
        }
        val inputStream = input.asInputStream()
        assertEquals(-1, inputStream.read())
        assertFalse(closed)
        inputStream.close()
        assertTrue(closed)
    }

    @Test
    fun testInputAsInputStreamRangeRead() {
        val inputStream = StringInput("abcdefgh").asInputStream()
        val bis = ByteArrayInputStream("abcdefgh".toByteArray())
        val array = ByteArray(100)
        val bisArray = ByteArray(100)
        run {
            assertEquals(bis.read(bisArray, 0, 0), inputStream.read(array, 0, 0))
            assertArrayEquals(bisArray, array)
        }

        run {
            assertEquals(bis.read(bisArray, 0, 1), inputStream.read(array, 0, 1))
            assertArrayEquals(bisArray, array)
        }

        run {
            assertEquals(bis.read(bisArray, 1, 2), inputStream.read(array, 1, 2))
            assertArrayEquals(bisArray, array)
        }

        run {
            assertEquals(bis.read(bisArray, 5, 90), inputStream.read(array, 5, 90))
            assertArrayEquals(bisArray, array)
        }
    }

    // IS as I

    @Test
    fun testInputStreamAsInput() {
        val inputStream = ByteArrayInputStream("1\n2".toByteArray())
        val lines = inputStream.asInput().readUtf8Lines()
        assertEquals(listOf("1", "2"), lines)
    }

    @Test
    fun testInputStreamAsInputBufferBoundary() {
        val baseline = "1".repeat(DEFAULT_BUFFER_SIZE + 1) + "\n" + "2".repeat(DEFAULT_BUFFER_SIZE + 1)
        val input = ByteArrayInputStream(baseline.toByteArray())
        val lines = input.asInput().readUtf8Lines()
        assertEquals(baseline.split("\n"), lines)
    }

    @Test
    fun testInputStreamAsInputNegativeValues() {
        val baseline = byteArrayOf(1, -1, -127, 0, -125, 127, -42, -1, 3)
        val inputStream = ByteArrayInputStream(baseline)
        val input = inputStream.asInput()
        val result = input.readByteArray()
        assertTrue(input.exhausted())
        assertArrayEquals(baseline, result)
    }

    @Test
    fun testEmptyInputStreamAsInput() {
        val input = ByteArrayInputStream(byteArrayOf()).asInput()
        assertTrue(input.exhausted())
    }

    @Test
    fun testInputStreamAsInputClose() {
        var closed = false
        val inputStream = object : InputStream() {
            override fun read(): Int {
                return -1
            }

            override fun close() {
                closed = true
            }
        }
        val input = inputStream.asInput()
        assertTrue(input.exhausted())
        assertFalse(closed)
        input.close()
        assertTrue(closed)
    }

    private val bytesSize = 1024 * 1024

    @Test
    fun testInputAsInputStreamRandomDataTest() {
        val content = Random.nextBytes(bytesSize)
        val inputStream = ByteArrayInput(content).asInputStream()
        val result = ByteArrayOutputStream()
        loop@ while (true) {
            when (Random.nextBoolean()) {
                true -> {
                    val b = inputStream.read()
                    if (b == -1) break@loop
                    result.write(b)
                }
                false -> {
                    val array = ByteArray(Random.nextInt(1, DEFAULT_BUFFER_SIZE * 2))
                    val offset = Random.nextInt(array.size)
                    val length = Random.nextInt(array.size - offset + 1)
                    val read = inputStream.read(array, offset, length)
                    if (read == -1) break@loop
                    result.write(array, offset, read)
                }
            }
        }
        assertArrayEquals(content, result.toByteArray())
    }

    @Test
    fun testInputStreamAsInputRandomDataTest() {
        val content = Random.nextBytes(bytesSize)
        val input = ByteArrayInputStream(content).asInput()
        val result = ByteArrayOutputStream()
        while (!input.exhausted()) {
            when (Random.nextBoolean()) {
                true -> {
                    result.write(input.readByte().toInt())
                }
                false -> {
                    val array = ByteArray(Random.nextInt(1, DEFAULT_BUFFER_SIZE * 2))
                    val offset = Random.nextInt(array.size)
                    val length = Random.nextInt(array.size - offset + 1)
                    val read = input.readAvailableTo(bufferOf(array), offset, offset + length)
                    result.write(array, offset, read)
                }
            }
        }
        assertArrayEquals(content, result.toByteArray())
    }
}
