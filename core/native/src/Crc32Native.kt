/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.io.unsafe.UnsafeByteArrayProcessor
import platform.zlib.crc32 as zlibCrc32

internal actual fun crc32(): Processor<Long> = Crc32Processor()

@OptIn(UnsafeIoApi::class)
private class Crc32Processor : UnsafeByteArrayProcessor<Long>() {
    private var crc: UInt = 0u

    @OptIn(UnsafeNumber::class)
    override fun process(source: ByteArray, startIndex: Int, endIndex: Int) {
        source.usePinned { pinned ->
            crc = zlibCrc32(crc.convert(), pinned.addressOf(startIndex).reinterpret(), (endIndex - startIndex).convert()).convert()
        }
    }

    override fun compute(): Long = crc.toLong()

    override fun close() {}
}
