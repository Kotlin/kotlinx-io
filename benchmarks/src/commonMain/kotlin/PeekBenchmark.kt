/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.Buffer
import kotlinx.io.Source

const val OFFSET_TO_LAST_BYTE_IN_SEGMENT = (SEGMENT_SIZE_IN_BYTES - 1).toString()

@State(Scope.Benchmark)
abstract class PeekBenchmark {
    protected val buffer = Buffer()

    // Use OFFSET_TO_LAST_BYTE_IN_SEGMENT to hit a border between
    // consecutive segments in benchmarks accessing multibyte values.
    @Param("0", OFFSET_TO_LAST_BYTE_IN_SEGMENT)
    var offset: Int = 0

    @Setup
    fun fillBuffer() {
        buffer.write(ByteArray(offset + 128))
    }

    protected fun peek(): Source {
        val peekSource = buffer.peek()
        peekSource.skip(offset.toLong())
        return peekSource
    }
}

@State(Scope.Benchmark)
open class PeekByteBenchmark : PeekBenchmark() {
    @Benchmark
    fun benchmark() = peek().readByte()
}

@State(Scope.Benchmark)
open class PeekShortBenchmark : PeekBenchmark() {
    @Benchmark
    fun benchmark() = peek().readShort()
}

@State(Scope.Benchmark)
open class PeekIntBenchmark : PeekBenchmark() {
    @Benchmark
    fun benchmark() = peek().readInt()
}

@State(Scope.Benchmark)
open class PeekLongBenchmark : PeekBenchmark() {
    @Benchmark
    fun benchmark() = peek().readLong()
}
