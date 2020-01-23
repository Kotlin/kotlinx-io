package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlin.test.*

class BufferTest {

    @Test
    fun testUsePointer() {
        val array = ByteArray(10)
        val buffer = bufferOf(array, 1, 8)

        var executed = false

        buffer.usePointer { bufferPointer ->
            array.usePinned { arrayPointer ->
                executed = true
                assertEquals(arrayPointer.addressOf(1), bufferPointer)
            }
        }

        assertTrue(executed)
    }
}