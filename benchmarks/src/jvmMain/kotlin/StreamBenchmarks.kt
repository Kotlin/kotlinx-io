/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.benchmark.*

open class InputStreamByteRead: BufferRWBenchmarkBase() {
   private val stream = buffer.inputStream()

    @Benchmark
    fun benchmark(): Int {
        buffer.writeByte(0)
        return stream.read()
    }
}

open class OutputStreamByteWrite: BufferRWBenchmarkBase() {
    private val stream = buffer.outputStream()

    @Benchmark
    fun benchmark(): Byte {
        stream.write(0)
        return buffer.readByte()
    }
}

abstract class StreamByteArrayBenchmarkBase: BufferRWBenchmarkBase() {
    protected var inputArray = ByteArray(0)
    protected var outputArray = ByteArray(0)

    @Param("1", "128", SEGMENT_SIZE_IN_BYTES.toString())
    var size: Int = 0

    @Setup
    fun setupArray() {
        inputArray = ByteArray(size)
        outputArray = ByteArray(size)
    }
}

open class InputStreamByteArrayRead: StreamByteArrayBenchmarkBase() {
    private val stream = buffer.inputStream()

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        buffer.write(inputArray)
        var offset = 0
        while (offset < outputArray.size) {
            offset += stream.read(outputArray, offset, outputArray.size - offset)
        }
        blackhole.consume(outputArray)
    }
}

open class OutputStreamByteArrayWrite: StreamByteArrayBenchmarkBase() {
    private val stream = buffer.outputStream()

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        stream.write(outputArray)
        buffer.readFully(inputArray)
        blackhole.consume(inputArray)
    }
}
