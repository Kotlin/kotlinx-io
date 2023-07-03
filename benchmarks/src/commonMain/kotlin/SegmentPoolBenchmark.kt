/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.io.*
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class SegmentPoolBenchmark {
    private val buffer = Buffer()

    @Benchmark
    fun acquireReleaseCycle() {
        // write will request a new segment
        buffer.writeByte(0)
        // clear will recycle an old segment
        buffer.clear()
    }
}
