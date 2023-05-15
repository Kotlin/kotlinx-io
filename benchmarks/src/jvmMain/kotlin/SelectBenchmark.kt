/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmark

import kotlinx.benchmark.*
import kotlinx.io.*
import kotlinx.io.ByteString.Companion.encodeUtf8
import org.openjdk.jmh.annotations.CompilerControl

@State(Scope.Benchmark)
open class SelectBenchmark {
    private val buffer = Buffer()

    private val shortOptions = Options.of(
        " ".encodeUtf8(), ",".encodeUtf8(),
        ".".encodeUtf8(), ":".encodeUtf8(),
        "?".encodeUtf8())
    private val shortData = " :?, .".toByteArray()

    private val notMatchingOptions = Options.of("hello".encodeUtf8())
    private val bufferForBailOutTest = Buffer().writeUtf8("world!")

    private val longScanOptions = Options.of("Pneumonoultramicroscopicsilicovolcanoconiosis".encodeUtf8())
    private val bufferForLongScanTest = Buffer().writeUtf8("Pneumonoultramicroscopicsilicovolcanoconiosi_")

    private var wideSelectOptions = Options.of()
    private val bufferForWideSelection = Buffer().writeUtf8("999999999")

    @Setup
    fun generateLongSelectOptions() {
        var prefix = ""
        // Create following list of options:
        // 0, 1, .., 8, 90, 91, .., 98, 990, ..., 9999999998
        val list = buildList<ByteString> {
            for (length in 0 .. 9) {
                for (digit in 0 .. 8) {
                    add((prefix + digit).encodeUtf8())
                }
                prefix += "9"
            }
        }
        wideSelectOptions = Options.of(*list.toTypedArray())
    }

    @Benchmark
    fun selectWithShortOptions(blackhole: Blackhole) {
        buffer.write(shortData)
        while (!buffer.exhausted()) {
            blackhole.consume(buffer.select(shortOptions))
        }
    }

    @Benchmark
    fun fastBailOut() = bufferForBailOutTest.select(notMatchingOptions)

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    fun longScan() = bufferForLongScanTest.select(longScanOptions)

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    fun wideSelect() = bufferForWideSelection.select(wideSelectOptions)

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    fun selectWithShortOptionsWithPeek(blackhole: Blackhole) {
        buffer.write(shortData)
        while (!buffer.exhausted()) {
            blackhole.consume(buffer.selectUsingPeekSource(shortOptions))
        }
    }

    @Benchmark
    fun fastBailOutWithPeek() = bufferForBailOutTest.selectUsingPeekSource(notMatchingOptions)

    @Benchmark
    fun longScanWithPeek() = bufferForLongScanTest.selectUsingPeekSource(longScanOptions)

    @Benchmark
    fun wideSelectWithPeek() = bufferForWideSelection.selectUsingPeekSource(wideSelectOptions)

    @Benchmark
    fun selectWithShortOptionsWithBufferedPeek(blackhole: Blackhole) {
        buffer.write(shortData)
        while (!buffer.exhausted()) {
            blackhole.consume(buffer.selectUsingBufferedPeekSource(shortOptions))
        }
    }

    @Benchmark
    fun fastBailOutWithBufferedPeek() = bufferForBailOutTest.selectUsingBufferedPeekSource(notMatchingOptions)

    @Benchmark
    fun longScanWithBufferedPeek() = bufferForLongScanTest.selectUsingBufferedPeekSource(longScanOptions)

    @Benchmark
    fun wideSelectWithBufferedPeek() = bufferForWideSelection.selectUsingBufferedPeekSource(wideSelectOptions)
}