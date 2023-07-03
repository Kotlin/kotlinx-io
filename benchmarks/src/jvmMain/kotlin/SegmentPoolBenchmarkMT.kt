/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.*
import org.openjdk.jmh.annotations.Group
import org.openjdk.jmh.annotations.GroupThreads

@State(Scope.Benchmark)
open class SegmentPoolBenchmarkMT {
    private fun testCycle(): Buffer {
        val buffer = Buffer()
        buffer.writeByte(0)
        buffer.clear()
        return buffer
    }

    @Benchmark
    @Group("ra1")
    @GroupThreads(1)
    fun acquireReleaseCycle() = testCycle()

    @Benchmark
    @Group("ra2")
    @GroupThreads(2)
    fun acquireReleaseCycle2() = testCycle()

    @Benchmark
    @Group("ra4")
    @GroupThreads(4)
    fun acquireReleaseCycle4() = testCycle()
}
