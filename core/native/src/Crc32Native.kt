/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.unsafe.withData
import platform.zlib.crc32 as zlibCrc32

internal actual fun crc32(): Processor<Long> = Crc32Processor()

@OptIn(UnsafeIoApi::class)
private class Crc32Processor : Processor<Long> {
    private var crc: UInt = 0u

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

                bytes.usePinned { pinned ->
                    crc = zlibCrc32(crc.convert(), pinned.addressOf(startIndex).reinterpret(), bytesToProcess.convert()).convert()
                }
                remaining -= bytesToProcess
            }
        }
    }

    override fun compute(): Long = crc.toLong()

    override fun close() {}
}
