package kotlinx.io.samples

import kotlinx.io.*
import platform.Foundation.NSInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals

class KotlinxIoSamplesApple {
    @Test
    fun inputStreamAsSource() {
        val data = ByteArray(100) { it.toByte() }
        val inputStream = NSInputStream(data.toNSData())

        val receivedData = inputStream.asSource().buffered().readByteArray()
        assertContentEquals(data, receivedData)
    }
}
