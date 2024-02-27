/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferAccessors
import kotlinx.io.unsafe.readFromHead
import kotlinx.io.unsafe.writeToTail

@State(Scope.Benchmark)
open class UnsafeApiBenchmarks {
    private val buffer = Buffer().apply { write(ByteArray(128)) }
    private val data = ByteArray(1024)

    @OptIn(UnsafeIoApi::class)
    @Benchmark
    fun readWrite() {
        var offset = 0
        val src = data
        do {
            UnsafeBufferAccessors.writeToTail(buffer, 1) { bb ->
                val len = minOf(bb.remaining(), src.size - offset)
                bb.put(src, offset, len)
                offset += len
            }
        } while (offset < src.size)

        offset = 0
        do {
            UnsafeBufferAccessors.readFromHead(buffer) { bb ->
                val len = minOf(bb.remaining(), src.size - offset)
                bb.get(src, offset, len)
                offset += len
            }
        } while (offset < src.size)
    }
}
