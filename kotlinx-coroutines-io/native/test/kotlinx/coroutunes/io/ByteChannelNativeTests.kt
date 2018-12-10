package kotlinx.coroutines.io

import kotlinx.io.core.*
import kotlinx.io.pool.*
import kotlinx.cinterop.*
import kotlin.test.*

class ByteChannelNativeTests : ByteChannelTestBase(true) {
    @Test
    fun testCPointersReadWriteFully() = runTest {
        val array = ByteArray(4)
        for (i in 0..3) {
            array[i] = i.toByte()
        }

        array.usePinned { pinned ->
            val ptr = pinned.addressOf(0)
            ch.writeFully(ptr, 0, 4)
        }

        val result = ByteArray(array.size)
        result.usePinned { pinned ->
            val ptr = pinned.addressOf(0)
            ch.readFully(ptr, 0, 4)
        }

        assertTrue { array.contentEquals(result) }
    }

    @Test
    fun testCPointersReadWriteFullyWithShift() = runTest {
        for (shift in 0 .. 3) {
            val array = ByteArray(4)
            for (i in 0..3) {
                array[i] = i.toByte()
            }

            array.usePinned { pinned ->
                val ptr = pinned.addressOf(0)
                ch.writeFully(ptr, shift, 4 - shift)
            }

            val result = ByteArray(array.size)
            result.usePinned { pinned ->
                val ptr = pinned.addressOf(0)
                ch.readFully(ptr, shift, 4 - shift)
            }

            for (i in shift .. 3) {
                assertEquals(array[i], result[i])
            }
        }
    }

    @Test
    fun testCPointersReadAvailable() = runTest {
        val array = ByteArray(4)
        for (i in 0..3) {
            array[i] = i.toByte()
        }

        array.usePinned { pinned ->
            val ptr = pinned.addressOf(0)
            ch.writeFully(ptr, 0, 4)
        }

        val result = ByteArray(array.size + 10)
        val size = result.usePinned { pinned ->
            val ptr = pinned.addressOf(0)
            ch.readAvailable(ptr, 0, result.size)
        }

        assertTrue { array.contentEquals(result.copyOf(size)) }
    }
}
