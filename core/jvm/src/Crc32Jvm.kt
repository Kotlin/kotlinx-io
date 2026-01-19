/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.unsafe.withData
import java.util.zip.CRC32

internal actual fun crc32(): Processor<Long> = Crc32Processor()

@OptIn(UnsafeIoApi::class)
private class Crc32Processor : Processor<Long> {
    private val crc32 = CRC32()

    override fun process(source: Buffer, byteCount: Long) {
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L || source.exhausted()) return

        val toProcess = minOf(byteCount, source.size)
        var remaining = toProcess

        UnsafeBufferOperations.forEachSegment(source) { context, segment ->
            if (remaining <= 0) return@forEachSegment

            context.withData(segment) { bytes, startIndex, endIndex ->
                val segmentSize = endIndex - startIndex
                val bytesToProcess = minOf(remaining, segmentSize.toLong()).toInt()
                crc32.update(bytes, startIndex, bytesToProcess)
                remaining -= bytesToProcess
            }
        }
    }

    override fun compute(): Long = crc32.value

    override fun close() {}
}
