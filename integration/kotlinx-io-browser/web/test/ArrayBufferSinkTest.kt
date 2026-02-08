package kotlinx.io.browser

import kotlinx.io.buffered
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.js.JsException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class ArrayBufferSinkTest {

    @Test
    fun writeToArrayBufferSinkIsReflectedInBuffer() {
        val expected = "Hello, world".encodeToByteArray()

        val rawSink = ArrayBufferSink(maxByteLength = 128)
        val bufferedSink = rawSink.buffered()
        bufferedSink.write(expected)
        bufferedSink.flush()

        assertContentEquals(expected, rawSink.arrayBuffer.toByteArray())
    }

    @Test
    fun arrayBufferSinkIsLimitedToMaxByteLength() {
        val rawSink = ArrayBufferSink(maxByteLength = 4)
        val bufferedSink = rawSink.buffered()
        bufferedSink.write("Hello, world".encodeToByteArray())
        assertFailsWith<JsException> { bufferedSink.flush() }
    }

    @Test
    fun writeToArrayBufferSinkAfterCloseFails() {
        val rawSink = ArrayBufferSink(maxByteLength = 128)
        val bufferedSink = rawSink.buffered()
        rawSink.close()
        assertFailsWith<IllegalStateException> {
            bufferedSink.write("Hello, world".encodeToByteArray())
            bufferedSink.flush()
        }
    }
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val jsArray = Int8Array(this)
    return ByteArray(getByteLength(this).toInt()) { index -> jsArray[index] }
}
