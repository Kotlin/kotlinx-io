/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.unsafe.UnsafeByteArrayProcessor
import java.util.zip.CRC32

internal actual fun crc32(): Processor<Long> = Crc32Processor()

@OptIn(UnsafeIoApi::class)
private class Crc32Processor : UnsafeByteArrayProcessor<Long>() {
    private val crc32 = CRC32()

    override fun process(source: ByteArray, startIndex: Int, endIndex: Int) {
        crc32.update(source, startIndex, endIndex - startIndex)
    }

    override fun compute(): Long = crc32.value

    override fun close() {}
}
