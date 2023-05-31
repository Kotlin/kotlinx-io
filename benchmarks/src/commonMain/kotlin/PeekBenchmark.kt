/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.io.*
import kotlinx.benchmark.*

@State(Scope.Benchmark)
abstract class PeekBenchmark {
    protected val buffer = Buffer()

    @Param("0", "8191")
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

open class PeekByteBenchmark : PeekBenchmark() {
    @Benchmark
    fun benchmark() = peek().readByte()
}

open class PeekShortBenchmark : PeekBenchmark() {
    @Benchmark
    fun benchmark() = peek().readShort()
}

open class PeekIntBenchmark : PeekBenchmark() {
    @Benchmark
    fun benchmark() = peek().readInt()
}

open class PeekLongBenchmark : PeekBenchmark() {
    @Benchmark
    fun benchmark() = peek().readLong()
}