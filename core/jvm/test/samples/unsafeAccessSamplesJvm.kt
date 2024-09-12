/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples.unsafe

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.toHexString
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import kotlinx.io.readString
import kotlinx.io.unsafe.*
import kotlinx.io.writeString
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import kotlin.random.Random
import kotlin.test.*

@OptIn(UnsafeIoApi::class)
class UnsafeReadWriteSamplesJvm {

    @Test
    fun writeToByteChannel() {
        val source = Buffer().apply { writeString("hello world") }
        // Open a file channel to write into.
        FileChannel.open(
            Files.createTempFile(null, null),
            StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE
        ).use { channel ->
            // Write data into the channel until source buffer exhausted
            while (!source.exhausted()) {
                // Take a byte buffer holding source's data prefix and send it to the channel
                UnsafeBufferOperations.readFromHead(source) { headByteBuffer: ByteBuffer ->
                    channel.write(headByteBuffer)
                }
            }
            assertEquals(11, channel.size())
        }
    }

    @Test
    fun readFromByteChannel() {
        val destination = Buffer()

        // Open a file channel
        FileChannel.open(
            Files.createTempFile(null, null),
            StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE
        ).use { channel ->
            // Write some data into it
            channel.write(ByteBuffer.wrap("hello world".encodeToByteArray()))
            // And reset a read position to the beginning of a file
            channel.position(0)

            // Read data until a channel exhausted
            var finished = false
            do {
                // Require a byte buffer to read data into.
                // By the end of the call,
                // all data written into that byte buffer will be appended to the destination buffer.
                UnsafeBufferOperations.writeToTail(destination, 1) { tailByteBuffer: ByteBuffer ->
                    val bytesRead = channel.read(tailByteBuffer)
                    // If we read nothing, it's time to wrap up.
                    finished = bytesRead <= 0
                }
            } while (!finished)
        }
        assertEquals("hello world", destination.readString())
    }

    @Test
    fun gatheringWrite() {
        // Pre allocate an array to hold byte buffers during readBulk call.
        // Such an array should be reused across multiple readBulk calls to reduce the number of allocations.
        val buffers = Array<ByteBuffer?>(16) { null }

        // A buffer to read from
        val source = Buffer().apply {  write(Random.nextBytes(64 * 1024)) }
        // Write the source buffer's content into a file using file channel's gathering write
        FileChannel.open(
            Files.createTempFile(null, null),
            StandardOpenOption.WRITE, StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE
        ).use { channel ->
            // Continue writing until the source is exhausted
            while (!source.exhausted()) {
                // Take as many byte buffers as possible (it depends on the source's size and the length
                // buffers array) and send it all to the channel.
                UnsafeBufferOperations.readBulk(source, buffers) { bbs: Array<ByteBuffer?>, byteBuffersCount: Int ->
                    val bytesWritten = channel.write(bbs, 0, byteBuffersCount)
                    // Corresponding number of bytes will be consumed from the buffer by the end of readBulk call
                    bytesWritten
                }
            }
            assertEquals(64 * 1024, channel.size())
        }
    }

    @OptIn(UnsafeByteStringApi::class, ExperimentalStdlibApi::class)
    @Test
    fun messageDigest() {
        fun Buffer.digest(algorithm: String): ByteString {
            val md = MessageDigest.getInstance(algorithm)
            // iterate over all segment and update data
            UnsafeBufferOperations.forEachSegment(this) { ctx, segment ->
                // when segment is null, we reached the end of a buffer
                // access segment data without copying it
                ctx.withData(segment) { data, startIndex, endIndex ->
                    md.update(data, startIndex, endIndex - startIndex)
                }
                // advance to the next segment
            }
            return UnsafeByteStringOperations.wrapUnsafe(md.digest())
        }

        val buffer = Buffer().also { it.writeString("hello world") }
        assertEquals("5eb63bbbe01eeed093cb22bb8f5acdc3", buffer.digest("MD5").toHexString())
    }
}
