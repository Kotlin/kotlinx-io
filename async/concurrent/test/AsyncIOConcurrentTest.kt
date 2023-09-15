/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsyncIOConcurrentTest {
    @Test
    fun asyncSourceAsBlocking() {
        val asyncSource = TestAsyncSource()
        asyncSource.buffer.writeString("hello")

        assertEquals("hello", asyncSource.asBlocking().buffered().readString())
    }

    @Test
    fun asyncSourceClose() {
        val asyncSource = TestAsyncSource()
        asyncSource.asBlocking().close()
        assertTrue(asyncSource.closed)
    }

    @Test
    fun asyncSinkAsBlocking() {
        val asyncSink = TestAsyncSink()

        asyncSink.asBlocking().buffered().apply {
            writeString("hello")
            flush()
        }

        assertTrue(asyncSink.flushed)
        assertEquals("hello", asyncSink.buffer.readString())
    }

    @Test
    fun asyncSinkClose() {
        val asyncSink = TestAsyncSink()

        asyncSink.asBlocking().close()
        assertTrue(asyncSink.closed)
    }
}
