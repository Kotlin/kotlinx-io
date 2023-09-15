/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.test.*

class PredicatesTest {
    @Test
    fun exhausted() = runTest {
        val predicate = AwaitPredicate.exhausted()
        var remainingCalls = 5
        val buffer = Buffer().apply { writeString("test") }
        assertTrue(predicate.apply(buffer) { remainingCalls-- > 0 })
        assertEquals("test", buffer.readString())
    }

    @Test
    fun available() = runTest {
        val targetBytesNumber = 17L
        val predicate = AwaitPredicate.available(targetBytesNumber)
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
    fun cacheAvailablePredicate() {
        for (i in 0 until 16) {
            assertSame(
                AwaitPredicate.available(1L.shl(i)),
                AwaitPredicate.available(1L.shl(i))
            )
        }
        for (i in 0 until 16) {
            for (j in 0 until 16) {
                if (i == j) continue

                assertNotSame(
                    AwaitPredicate.available(1L.shl(i)),
                    AwaitPredicate.available(1L.shl(j))
                )
            }
        }
    }

    @Test
    fun noFetchIfBufferContainsData() = runTest {
        val predicate = AwaitPredicate.available(8)
        val buffer = Buffer().apply { write(ByteArray(8)) }
        assertTrue(predicate.apply(buffer) {
            fail("Fetch should be invoked.")
        })
    }

    @Test
    fun exhaustedBeforeBufferFilled() = runTest {
        val predicate = AwaitPredicate.available(Long.MAX_VALUE)
        val buffer = Buffer()
        assertFalse(predicate.apply(buffer) { false })
    }

    @Test
    fun bytePresentedPredicate() = runTest {
        val str = "hello async world"
        val buffer = Buffer().apply { writeString(str) }

        assertFalse(AwaitPredicate.contains('!'.code.toByte()).apply(buffer) { false })
        assertTrue(AwaitPredicate.contains(' '.code.toByte()).apply(buffer) { false })
        assertFalse(AwaitPredicate.contains(' '.code.toByte(), str.indexOf(' ').toLong())
            .apply(buffer) { false })
        assertTrue(AwaitPredicate.contains(' '.code.toByte(), str.indexOf(' ').toLong() + 1L)
            .apply(buffer) { false })

        assertEquals(str, buffer.readString())
    }

    @Test
    fun fetchDataUntilByteFound() = runTest {
        val buffer = Buffer()
        var fetches = 0
        assertTrue(AwaitPredicate.contains(0x42).apply(buffer) {
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
    fun bytesPresentedPredicates() = runTest {
        val str = "hello async world"
        val buffer = Buffer().apply { writeString(str) }

        assertFalse(AwaitPredicate.contains("test".encodeToByteString()).apply(buffer) { false })
        assertTrue(AwaitPredicate.contains("sync".encodeToByteString()).apply(buffer) { false })
        assertFalse(AwaitPredicate.contains("sync".encodeToByteString(), 5L).apply(buffer) { false })
    }

    @Test
    fun fetchDataUntilBytesFound() = runTest {
        val buffer = Buffer()
        var fetches = 0
        assertTrue(AwaitPredicate.contains("found!".encodeToByteString()).apply(buffer) {
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

    @Test
    fun newline() = runTest {
        val buffer = Buffer().apply { writeString("hello\nworld") }
        assertTrue(AwaitPredicate.newLine().apply(buffer) { false })
        assertFalse(AwaitPredicate.newLine(5).apply(buffer) { false })
        assertTrue(AwaitPredicate.newLine(6).apply(buffer) { false })
        assertFalse(AwaitPredicate.newLine().apply(Buffer()) { false })
    }


    @Test
    fun invalidParameters() {
        assertFailsWith<IllegalArgumentException> { AwaitPredicate.available(-1) }
        assertFailsWith<IllegalArgumentException> { AwaitPredicate.contains(42, 0) }
        assertFailsWith<IllegalArgumentException> { AwaitPredicate.contains(42, -1) }
        assertFailsWith<IllegalArgumentException> { AwaitPredicate.contains(ByteString(1, 2, 3), 2) }
        assertFailsWith<IllegalArgumentException> { AwaitPredicate.contains(ByteString(1, 2, 3), -1) }
        assertFailsWith<IllegalArgumentException> { AwaitPredicate.contains(ByteString()) }
        assertFailsWith<IllegalArgumentException> { AwaitPredicate.newLine(0) }
    }
}
