/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.NSInputStream
import platform.Foundation.NSStreamStatusClosed
import platform.Foundation.NSStreamStatusNotOpen
import platform.Foundation.NSStreamStatusOpen
import platform.darwin.NSUIntegerVar
import platform.darwin.UInt8Var
import kotlin.test.*

@OptIn(UnsafeNumber::class)
class SourceNSInputStreamTest {
    @Test
    fun bufferInputStream() {
        val source = Buffer()
        source.writeString("abc")
        testInputStream(source.asNSInputStream())
    }

    @Test
    fun realBufferedSourceInputStream() {
        val source = Buffer()
        source.writeString("abc")
        testInputStream(RealSource(source).asNSInputStream())
    }

    private fun testInputStream(nsis: NSInputStream) {
        val byteArray = ByteArray(4)
        byteArray.usePinned {
            val cPtr = it.addressOf(0).reinterpret<UInt8Var>()

            assertEquals(NSStreamStatusNotOpen, nsis.streamStatus)
            assertEquals(-1, nsis.read(cPtr, 4U))
            nsis.open()
            assertEquals(NSStreamStatusOpen, nsis.streamStatus)

            byteArray.fill(-5)
            assertEquals(3, nsis.read(cPtr, 4U))
            assertEquals("[97, 98, 99, -5]", byteArray.contentToString())

            byteArray.fill(-7)
            assertEquals(0, nsis.read(cPtr, 4U))
            assertEquals("[-7, -7, -7, -7]", byteArray.contentToString())
        }
    }

    @Test
    fun nsInputStreamGetBuffer() {
        val source = Buffer()
        source.writeString("abc")

        val nsis = source.asNSInputStream()
        nsis.open()
        assertTrue(nsis.hasBytesAvailable)

        memScoped {
            val bufferPtr = alloc<CPointerVar<UInt8Var>>()
            val lengthPtr = alloc<NSUIntegerVar>()
            assertTrue(nsis.getBuffer(bufferPtr.ptr, lengthPtr.ptr))

            val length = lengthPtr.value
            assertNotNull(length)
            assertEquals(3.convert(), length)

            val buffer = bufferPtr.value
            assertNotNull(buffer)
            assertEquals('a'.code.convert(), buffer[0])
            assertEquals('b'.code.convert(), buffer[1])
            assertEquals('c'.code.convert(), buffer[2])
        }
    }

    @Test
    fun nsInputStreamClose() {
        val buffer = Buffer()
        buffer.writeString("abc")
        val source = RealSource(buffer)
        assertFalse(source.closed)

        val nsis = source.asNSInputStream()
        nsis.open()
        nsis.close()
        assertTrue(source.closed)
        assertEquals(NSStreamStatusClosed, nsis.streamStatus)

        val byteArray = ByteArray(4)
        byteArray.usePinned {
            val cPtr = it.addressOf(0).reinterpret<UInt8Var>()

            byteArray.fill(-5)
            assertEquals(-1, nsis.read(cPtr, 4U))
            assertNotNull(nsis.streamError)
            assertEquals("Underlying source is closed.", nsis.streamError?.localizedDescription)
            assertEquals("[-5, -5, -5, -5]", byteArray.contentToString())
        }
    }
}
