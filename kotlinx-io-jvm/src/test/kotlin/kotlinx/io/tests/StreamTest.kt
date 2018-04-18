package kotlinx.io.tests

import kotlinx.io.core.*
import kotlinx.io.streams.*
import org.junit.Test
import java.io.*
import java.util.concurrent.atomic.*
import kotlin.concurrent.*
import kotlin.test.*

class StreamTest {
    @Test
    fun testOutput() {
        val baos = ByteArrayOutputStream()
        val output = baos.asOutput()

        output.byteOrder = ByteOrder.BIG_ENDIAN
        output.writeInt(0x11223344)

        output.flush()

        assertTrue { byteArrayOf(0x11, 0x22, 0x33, 0x44).contentEquals(baos.toByteArray()) }
    }

    @Test
    fun testInput() {
        val baos = ByteArrayInputStream(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        val input = baos.asInput()

        input.byteOrder = ByteOrder.BIG_ENDIAN
        assertEquals(0x11223344, input.readInt())
    }

    @Test
    fun testInputParts() {
        val inputStream = PipedInputStream()
        val outputStream = PipedOutputStream()

        outputStream.connect(inputStream)

        outputStream.write(byteArrayOf(0x11, 0x22))
        val input = inputStream.asInput()

        val th = thread(start = false, name = "testInputParts") {
            input.byteOrder = ByteOrder.BIG_ENDIAN
            assertEquals(0x11223344, input.readInt())
        }

        val failed = AtomicReference<Throwable?>()
        th.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            failed.set(e)
        }

        th.start()

        while (true) {
            Thread.sleep(100)
            if (th.state === Thread.State.BLOCKED || th.state === Thread.State.WAITING || th.state === Thread.State.TIMED_WAITING) break
        }

        outputStream.write(byteArrayOf(0x33, 0x44))
        outputStream.close()

        try {
            th.join()
        } catch (e: Throwable) {
            th.interrupt()
            inputStream.close()
            outputStream.close()
        }

        failed.get()?.let { throw it }
    }
}
