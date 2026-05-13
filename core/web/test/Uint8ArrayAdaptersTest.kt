@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package kotlinx.io.browser

import kotlinx.io.*
import kotlinx.io.external.ArrayBuffer
import kotlinx.io.external.Uint8Array
import kotlinx.io.external.get
import kotlinx.io.external.set
import kotlin.js.js
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Uint8ArrayAdaptersTest {

    @Test
    fun uint8ArraySinkWriteReflectedInBuffer() {
        val expected = "Hello, world".encodeToByteArray()

        val array = Uint8Array(resizableArrayBuffer(maxByteLength = 128))
        val sink = array.asSink().buffered()
        sink.write(expected)
        sink.flush()

        assertContentEquals(expected, array.toByteArray())
    }

    @Test
    fun uint8ArraySinkWriteLimitedByMaxByteLength() {
        val array = Uint8Array(resizableArrayBuffer(maxByteLength = 4))
        val sink = array.asSink().buffered()
        sink.write("Hello, world".encodeToByteArray())
        assertFailsWith<IOException> { sink.flush() }
    }

    @Test
    fun uint8ArraySinkWriteFailsAfterClose() {
        val array = Uint8Array(resizableArrayBuffer(maxByteLength = 128))
        val rawSink = array.asSink()
        val bufferedSink = rawSink.buffered()
        rawSink.close()
        assertFailsWith<IllegalStateException> {
            bufferedSink.write("Hello, world".encodeToByteArray())
            bufferedSink.flush()
        }
    }

    @Test
    fun partialUint8ArraySourceRead() {
        val input = byteArrayOf(1, 2, 3, 4)
        val source = input.toUint8Array().asSource()
        val buffer = Buffer()
        assertEquals(3, source.readAtMostTo(buffer, 3))
        assertEquals(1, source.readAtMostTo(buffer, 3))
        assertEquals(-1, source.readAtMostTo(buffer, 3))

        assertContentEquals(input, buffer.readByteArray())
    }

    @Test
    fun exhaustiveUint8ArraySourceRead() {
        val expected = byteArrayOf(1, 2, 3, 4)
        val source = expected.toUint8Array().asSource().buffered()
        assertContentEquals(expected, source.readByteArray())
    }

    @Test
    fun readFromUint8ArraySourceFailsAfterClose() {
        val source = byteArrayOf(1, 2, 3, 4).toUint8Array().asSource()
        source.close()

        assertFailsWith<IllegalStateException> {
            source.buffered().readByteArray()
        }
    }
}

private fun resizableArrayBuffer(maxByteLength: Int): ArrayBuffer =
    js("new ArrayBuffer(0, { maxByteLength: maxByteLength })")


private fun Uint8Array.toByteArray(): ByteArray =
    ByteArray(length.toInt()) { index -> this[index.toDouble()] }


private fun ByteArray.toUint8Array(): Uint8Array {
    val jsArray = Uint8Array(ArrayBuffer(size.toDouble()))
    for (i in 0 until size) {
        jsArray[i.toDouble()] = this[i]
    }
    return jsArray
}
