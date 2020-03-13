@file:Suppress("FORBIDDEN_IDENTITY_EQUALS")
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
    fun testDiscardExactOnEmpty() {
        assertFails {
            EmptyInput.discardExact(1)
        }
    }

    @Test
    fun testDiscardOnEmpty() {
        assertEquals(0, EmptyInput.discard(1))
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
        checkException { ErrorInput.discardExact(1) }
        checkException { ErrorInput.exhausted() }
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

        assertTrue(input.exhausted())
        assertEquals(content.size, count)

        assertArrayEquals(content, output.toByteArray())
    }

    @Test
    fun testInputCopyToWithSize(): Unit = listOf(0, 1, 128 + 1, 1024 + 1, 4096 + 1).forEach { size ->
        val content = ByteArray(8 * 1024) { it.toByte() }
        val input = ByteArrayInput(content)
        val output = ByteArrayOutput()

        val count = input.copyTo(output, size)

        assertTrue(!input.exhausted())
        assertEquals(size, count)

        assertArrayEquals(content.sliceArray(0 until size), output.toByteArray())
    }

    @Test
    fun testReadAvailableToRange() {
        var executed = false
        val input: Input = object : Input() {
            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                assertEquals(1024, endIndex)
                executed = true
                return endIndex - startIndex
            }

            override fun closeSource() {
            }

        }
        val buffer = bufferOf(ByteArray(1024))
        val end = input.readAvailableTo(buffer, 1)
        assertTrue(executed)
        assertEquals(1023, end)
    }

    @Test
    fun testReadAvailableToReturnValue() {
        var readIndex = 0
        var writeIndex = 0

        val input = object : Input() {
            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                readIndex++

                buffer.storeByteAt(startIndex, 42)
                return 1
            }

            override fun closeSource() {
                return
            }
        }

        val output = object : Output() {
            override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
                writeIndex++

                assertEquals(startIndex + 1, endIndex)
                assertEquals(42, source.loadByteAt(startIndex))
            }

            override fun closeSource() {
            }
        }

        repeat(DEFAULT_BUFFER_SIZE * 2) {
            assertEquals(1, input.readAvailableTo(output))
            output.flush()
            assertEquals(it + 1, readIndex)
            assertEquals(it + 1, writeIndex)
        }

        repeat(DEFAULT_BUFFER_SIZE * 2) {
            input.prefetch(1)
            assertEquals(1, input.readAvailableTo(output))

            assertEquals(DEFAULT_BUFFER_SIZE * 2 + it + 1, readIndex)
            assertEquals(DEFAULT_BUFFER_SIZE * 2 + it + 1, writeIndex)
        }
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
