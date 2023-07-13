package kotlinx.io.samples

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.reinterpret
import kotlinx.io.*
import platform.Foundation.*
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class KotlinxIoSamplesApple {
    @Test
    fun inputStreamAsSource() {
        val data = ByteArray(100) { it.toByte() }
        val inputStream = NSInputStream(data.toNSData())

        val receivedData = inputStream.asSource().buffered().readByteArray()
        assertContentEquals(data, receivedData)
    }

    @Test
    fun outputStreamAsSink() {
        val data = ByteArray(100) { it.toByte() }
        val outputStream = NSOutputStream.outputStreamToMemory()

        val sink = outputStream.asSink().buffered()
        sink.write(data)
        sink.flush()

        val writtenData = outputStream.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData
        assertContentEquals(data, writtenData.toByteArray())
    }

    @Test
    @OptIn(UnsafeNumber::class)
    fun asStream() {
        val buffer = Buffer()
        val data = ByteArray(100) { it.toByte() }.toNSData()

        val outputStream = buffer.asNSOutputStream()
        outputStream.open()
        outputStream.write(data.bytes?.reinterpret(), data.length)

        val inputStream = buffer.asNSInputStream()
        inputStream.open()
        val readData = NSMutableData.create(length = 100.convert())!!
        inputStream.read(readData.bytes?.reinterpret(), 100.convert())

        assertContentEquals(data.toByteArray(), readData.toByteArray())
    }
}
