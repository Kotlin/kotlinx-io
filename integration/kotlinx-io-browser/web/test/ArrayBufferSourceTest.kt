package kotlinx.io.browser

import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.set
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

private const val BUFFERED_READ_SIZE = 8192

class ArrayBufferSourceTest {

    @Test
    fun readPartialArrayBufferSource() {
        val input = ByteArray(BUFFERED_READ_SIZE + 4) { index ->
            if (index <= BUFFERED_READ_SIZE) 0 else (index - BUFFERED_READ_SIZE).toByte()
        }
        val expected = byteArrayOf(0, 1, 2, 3)
        val source = ArrayBufferSource(input.toArrayBuffer()).buffered()

        source.skip(BUFFERED_READ_SIZE.toLong())
        assertContentEquals(expected, source.readByteArray())
    }

    @Test
    fun readEntireArrayBufferSource() {
        val expected = byteArrayOf(1, 2, 3, 4)
        val source = ArrayBufferSource(expected.toArrayBuffer()).buffered()
        assertContentEquals(expected, source.readByteArray())
    }

    @Test
    fun readFromArrayBufferSourceAfterCloseFails() {
        val source = ArrayBufferSource(ArrayBuffer(0))
        source.close()

        assertFailsWith<IllegalStateException> {
            source.readAtMostTo(Buffer(), 1)
        }
    }
}

private fun ByteArray.toArrayBuffer(): ArrayBuffer {
    val arrayBuffer = ArrayBuffer(size)
    val jsArray = Int8Array(arrayBuffer)
    for (i in 0 until size) {
        jsArray[i] = this[i]
    }
    return arrayBuffer
}
