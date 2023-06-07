/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import java.nio.ByteBuffer

import kotlinx.benchmark.*

open class ByteBufferReadWrite: BufferRWBenchmarkBase() {
    private var inputBuffer = ByteBuffer.allocate(0)
    private var outputBuffer = ByteBuffer.allocate(0)

    @Param("1", "1024", (SEGMENT_SIZE_IN_BYTES * 3).toString())
    var size: Int = 0

    @Setup
    fun allocateBuffers() {
        inputBuffer = ByteBuffer.allocate(size)
        inputBuffer.put(ByteArray(size))
        outputBuffer = ByteBuffer.allocate(size)
    }

    @Benchmark
    fun benchmark(): ByteBuffer {
        inputBuffer.rewind()
        outputBuffer.clear()
        buffer.write(inputBuffer)
        while (buffer.read(outputBuffer) > 0) {
            // do nothing
        }
        return outputBuffer
    }
}