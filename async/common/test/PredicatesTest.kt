/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.test.*

class PredicatesTest {
    @Test
    fun testExhaustedPredicate() = runTest {
        val predicate = AwaitPredicate.exhausted()
        var remainingCalls = 5
        val buffer = Buffer().apply { writeString("test") }
        assertTrue(predicate.apply(buffer) { remainingCalls-- > 0 })
        assertEquals("test", buffer.readString())
    }

    @Test
    fun testBytesAvailable() = runTest {
        val targetBytesNumber = 17L
        val predicate = AwaitPredicate.dataAvailable(targetBytesNumber)
        val buffer = Buffer()
        var bytesWritten = 0L
        assertTrue(predicate.apply(buffer) {
            buffer.writeByte(0)
            bytesWritten++
            true
        })
        assertEquals(targetBytesNumber, bytesWritten)
    }

    @Test
    fun testCacheDataAvailablePredicate() {
        for (i in 0 until 16) {
            assertSame(
                AwaitPredicate.dataAvailable(1L.shl(i)),
                AwaitPredicate.dataAvailable(1L.shl(i))
            )
        }
        for (i in 0 until 16) {
            for (j in 0 until 16) {
                if (i == j) continue

                assertNotSame(
                    AwaitPredicate.dataAvailable(1L.shl(i)),
                    AwaitPredicate.dataAvailable(1L.shl(j))
                )
            }
        }
    }

    @Test
    fun testNoFetchIfBufferContainsData() = runTest {
        val predicate = AwaitPredicate.dataAvailable(8)
        val buffer = Buffer().apply { write(ByteArray(8)) }
        assertTrue(predicate.apply(buffer) {
            fail("Fetch should be invoked.")
        })
    }

    @Test
    fun testExhaustedBeforeBufferFilled() = runTest {
        val predicate = AwaitPredicate.dataAvailable(Long.MAX_VALUE)
        val buffer = Buffer()
        assertFalse(predicate.apply(buffer) { false })
    }

    @Test
    fun testBytePresentedPredicate() = runTest {
        val str = "hello async world"
        val buffer = Buffer().apply { writeString(str) }

        assertFalse(AwaitPredicate.byteFound('!'.code.toByte()).apply(buffer) { false })
        assertTrue(AwaitPredicate.byteFound(' '.code.toByte()).apply(buffer) { false })
        assertFalse(AwaitPredicate.byteFound(' '.code.toByte(), str.indexOf(' ').toLong())
            .apply(buffer) { false })
        assertTrue(AwaitPredicate.byteFound(' '.code.toByte(), str.indexOf(' ').toLong() + 1L)
            .apply(buffer) { false })

        assertEquals(str, buffer.readString())
    }

    @Test
    fun testFetchDataUntilByteFound() = runTest {
        val buffer = Buffer()
        var fetches = 0
        assertTrue(AwaitPredicate.byteFound(0x42).apply(buffer) {
            fetches++
            if (fetches == 3) {
                buffer.writeByte(0x42)
            } else {
                buffer.writeString("wait!")
            }
            true
        })
        assertEquals(3, fetches)
    }

    @Test
    fun testBytesPresentedPredicates() = runTest {
        val str = "hello async world"
        val buffer = Buffer().apply { writeString(str) }

        assertFalse(AwaitPredicate.bytesFound("test".encodeToByteString()).apply(buffer) { false })
        assertTrue(AwaitPredicate.bytesFound("sync".encodeToByteString()).apply(buffer) { false })
        assertFalse(AwaitPredicate.bytesFound("sync".encodeToByteString(), 5L).apply(buffer) { false })
    }

    @Test
    fun testFetchDataUntilBytesFound() = runTest {
        val buffer = Buffer()
        var fetches = 0
        assertTrue(AwaitPredicate.bytesFound("found!".encodeToByteString()).apply(buffer) {
            fetches++
            if (fetches == 3) {
                buffer.writeString("found! ")
            } else {
                buffer.writeString("not yet ")
            }
            true
        })
        assertEquals(3, fetches)
    }
}
