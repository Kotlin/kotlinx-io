package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.pool.*
import kotlin.test.*

class InputOutputTest {

    val EmptyInput = object : Input() {
        override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
            return 0
        }

        override fun closeSource() {
        }
    }

    val error = IllegalStateException("Custom fill error")

    val ErrorInput = object : Input() {
        override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
            throw error
        }

        override fun closeSource() {
        }
    }

    @Test
    fun testReadAvailableToWithSameBuffer() {
        var instance: Buffer = Buffer.EMPTY
        var result: Buffer = Buffer.EMPTY

        val input: Input = LambdaInput { buffer, start, end ->
            instance = buffer
            return@LambdaInput 42
        }

        val output = LambdaOutput { source, startIndex, endIndex ->
            result = source
            assertEquals(42, endIndex)
        }

        input.readAvailableTo(output)
        output.flush()

        assertNotNull(instance)
        assertTrue(instance === result)
    }


    @Test
    fun testFillDirect() {
        val myBuffer = bufferOf(ByteArray(1024))
        val input = object : Input() {
            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                assertTrue { myBuffer === buffer }
                buffer[startIndex] = 42
                return 1
            }

            override fun closeSource() {

            }
        }

        assertEquals(1, input.readAvailableTo(myBuffer))
    }

    @Test
    fun testDiscardOnEmpty() {
        assertFails {
            EmptyInput.discard(1)
        }
    }

    @Test
    fun testPrefetchOnEmpty() {
        assertFalse { EmptyInput.prefetch(1) }
    }

    @Test
    fun testPreviewOnEmpty() {
        assertFails { EmptyInput.preview { } }
    }

    @Test
    fun testBypassFillExceptions() {
        checkException { ErrorInput.readByte() }
        checkException { ErrorInput.preview { } }
        checkException { ErrorInput.prefetch(1) }
        checkException { ErrorInput.discard(1) }
        checkException { ErrorInput.eof() }
        checkException {
            ErrorInput.readAvailableTo(
                object : Output() {
                    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
                        error("flush")
                    }

                    override fun closeSource() {
                        error("close")
                    }
                }
            )
        }

        checkException { ErrorInput.readAvailableTo(bufferOf(ByteArray(10))) }

        ErrorInput.close()
    }

    @Test
    fun testInputCopyTo() {
        val content = ByteArray(1024) { it.toByte() }
        val input = ByteArrayInput(content)
        val output = ByteArrayOutput()

        val count = input.copyTo(output)

        assertTrue(input.eof())
        assertEquals(content.size, count)

        assertArrayEquals(content, output.toByteArray())
    }

    @Test
    fun testInputCopyToWithSize(): Unit = listOf(0, 1, 128 + 1, 1024 + 1, 4096 + 1).forEach { size ->
        val content = ByteArray(8 * 1024) { it.toByte() }
        val input = ByteArrayInput(content)
        val output = ByteArrayOutput()

        val count = input.copyTo(output, size)

        assertTrue(!input.eof())
        assertEquals(size, count)

        assertArrayEquals(content.sliceArray(0 until size), output.toByteArray())
    }

    private fun checkException(block: () -> Unit) {
        var fail = false
        try {
            block()
        } catch (exception: Throwable) {
            fail = true
            assertEquals(error, exception)
        }

        assertTrue(fail)
    }
}

internal class SingleShotPool(private val buffer: Buffer) : DefaultPool<Buffer>(1) {
    private var produced = false
    private var disposed = false

    override fun produceInstance(): Buffer {
        produced = true
        return buffer
    }

    override fun disposeInstance(instance: Buffer) {
        assertFalse(disposed)
        disposed = false

        assertTrue { buffer === instance }
    }
}
