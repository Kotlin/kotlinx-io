/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmark

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.io.Buffer
import org.openjdk.jmh.annotations.CompilerControl

@State(Scope.Benchmark)
abstract class TypeProfileBenchmarkBase<T> {
    protected var buffer0 = Buffer()
    protected var buffer1 = Buffer()
    protected var buffer2 = Buffer()
    protected var buffer3 = Buffer()

    abstract fun benchmarkImpl(buffer: Buffer): T

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    fun warmup_0(): T = benchmarkImpl(buffer1)

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    fun warmup_1(): T = benchmarkImpl(buffer2)

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    fun warmup_2(): T = benchmarkImpl(buffer3)

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    fun benchmark(): T = benchmarkImpl(buffer0)
}

@State(Scope.Benchmark)
open class TypeProfileReadWriteByteWithDifferentSegments : TypeProfileBenchmarkBase<Byte>() {
    override fun benchmarkImpl(buffer: Buffer): Byte = buffer.writeByte(42).readByte()
}

@State(Scope.Benchmark)
open class TypeProfileReadWriteLongWithDifferentSegments : TypeProfileBenchmarkBase<Long>() {
    override fun benchmarkImpl(buffer: Buffer): Long = buffer.writeLong(42).readLong()
}

@State(Scope.Benchmark)
open class TypeProfileReadWriteByteWithDifferentPools : TypeProfileBenchmarkBase<Byte>() {
    override fun benchmarkImpl(buffer: Buffer): Byte = buffer.writeByte(42).readByte()
}

@State(Scope.Benchmark)
open class TypeProfileReadWriteLongWithDifferentPools : TypeProfileBenchmarkBase<Long>() {
    override fun benchmarkImpl(buffer: Buffer): Long = buffer.writeLong(42).readLong()
}