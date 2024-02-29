/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples.unsafe

import kotlinx.io.*
import kotlinx.io.unsafe.UnsafeBufferAccessors
import kotlinx.io.unsafe.readFromHead
import kotlinx.io.unsafe.readFully
import kotlinx.io.unsafe.writeToTail
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsafeReadWriteSamplesJvm {
    @OptIn(UnsafeIoApi::class, SnapshotApi::class)
    @Test
    fun writeToByteChannel() {
        val buffer = Buffer().apply { writeString("hello world") }
        FileChannel.open(
            Files.createTempFile(null, null),
            StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE
        ).use { channel ->
            while (!buffer.exhausted()) {
                UnsafeBufferAccessors.readFromHead(buffer) { bb ->
                    channel.write(bb)
                }
            }
            assertEquals(11, channel.size())
        }
    }

    @OptIn(UnsafeIoApi::class, SnapshotApi::class)
    @Test
    fun readFromByteChannel() {
        val buffer = Buffer()

        FileChannel.open(
            Files.createTempFile(null, null),
            StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE
        ).use { channel ->
            channel.write(ByteBuffer.wrap("hello world".encodeToByteArray()))
            channel.position(0)

            var finished = false
            do {
                UnsafeBufferAccessors.writeToTail(buffer, 1) { bb ->
                    val bytesRead = channel.read(bb)
                    finished = bytesRead <= 0
                }
            } while (!finished)
        }
        assertEquals("hello world", buffer.readString())
    }

    @OptIn(SnapshotApi::class, UnsafeIoApi::class)
    @Test
    fun gatheringWrite() {
        val buffers = Array<ByteBuffer?>(16) { null }

        val buffer = Buffer().apply {
            write(Random.nextBytes(64 * 1024))
        }
        FileChannel.open(
            Files.createTempFile(null, null),
            StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE
        ).use { channel ->
            while (!buffer.exhausted()) {
                UnsafeBufferAccessors.readFully(buffer, buffers) { bbs, from, to ->
                    channel.write(bbs, from, to)
                }
            }
            assertEquals(64 * 1024, channel.size())
        }
    }
}
