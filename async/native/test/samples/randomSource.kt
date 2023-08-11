/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async.samples

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.async.AsyncRawSource
import kotlinx.io.async.AwaitPredicate
import kotlinx.io.async.buffered
import kotlinx.io.async.use
import kotlinx.io.readByteString
import platform.posix.*
import kotlin.math.min
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalForeignApi::class)
class AsyncRandomSource : AsyncRawSource {
    private val fd: Int = open("/tmp/random", O_RDONLY).apply {
        if (this < 0) {
            throw IOException("Failed to open /dev/random: ${strerror(errno)?.toKString()}")
        }
    }

    private val internalBuffer = ByteArray(1024)

    @OptIn(UnsafeNumber::class)
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        val capacity = min(byteCount, internalBuffer.size.toLong())
        val bytesRead: Int = withContext(Dispatchers.IO) {
            internalBuffer.usePinned {
                val x = read(fd, it.addressOf(0), capacity.convert())
                return@withContext x.convert()
            }
        }
        if (bytesRead > 0) {
            sink.write(internalBuffer, 0, bytesRead)
        }
        return bytesRead.toLong()
    }

    override fun closeAbruptly() {
        close(fd)
    }

    override suspend fun close() {
        close(fd)
    }
}

class RandomSourceTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testRead() = runTest(timeout = 2L.minutes) {
        AsyncRandomSource().buffered().use {
            it.await(AwaitPredicate.dataAvailable(1000000))
            println("${it.buffer.size} bytes available: ${it.buffer.readByteString(10)}")
        }
    }
}
