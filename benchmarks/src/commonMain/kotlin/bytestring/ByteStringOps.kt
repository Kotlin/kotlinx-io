/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.bytestring.*
import kotlin.math.min

const val TARGET_BYTE: Byte = 42

@State(Scope.Benchmark)
abstract class IndexOfByteBenchmarkBase {
    @Param("128:-1", "128:0", "128:127", "128:63")
    var params: String = "<byte string size>:<target byte offset, or -1>"

    protected var byteString = ByteString()

    @Setup
    fun setupByteString() {
        val paramsParsed = params.split(':').map { it.toInt() }.toIntArray()
        require(paramsParsed.size == 2)
        val size = paramsParsed[0]
        val targetByteIndex = paramsParsed[1]


        require(targetByteIndex == -1 || targetByteIndex in 0 until size)
        val data = ByteArray(size)
        if (targetByteIndex >= 0) {
            data[targetByteIndex] = TARGET_BYTE
        }
        byteString = ByteString(data)
    }
}

@State(Scope.Benchmark)
open class IndexOfByteBenchmark : IndexOfByteBenchmarkBase() {
    @Benchmark
    fun benchmark() = byteString.indexOf(TARGET_BYTE)
}

@State(Scope.Benchmark)
open class LastIndexOfByteBenchmark : IndexOfByteBenchmarkBase() {
    @Benchmark
    fun benchmark() = byteString.lastIndexOf(TARGET_BYTE)
}

@State(Scope.Benchmark)
abstract class IndexOfByteStringBase {
    @Param("128:8:-1", "128:129:0", "128:8:0", "128:8:120", "128:8:63")
    var params: String = "<byte string size>:<target byte string size>:<target byte string offset, or -1>"

    protected var byteString = ByteString()

    protected var targetByteString = ByteString()

    @Setup
    fun setupByteString() {
        val paramsParsed = params.split(':').map { it.toInt() }.toIntArray()
        require(paramsParsed.size == 3)
        val size = paramsParsed[0]
        val patternLength = paramsParsed[1]
        val targetValueOffset = paramsParsed[2]

        require(size > 0)
        require(targetValueOffset == -1 || targetValueOffset in 0 until size)

        val data = ByteArray(size)
        if (targetValueOffset != -1) {
            for (idx in targetValueOffset until min(size, targetValueOffset + patternLength)) {
                data[idx] = TARGET_BYTE
            }
        }
        byteString = ByteString(data)

        targetByteString = ByteString(ByteArray(patternLength) { TARGET_BYTE })
    }
}

@State(Scope.Benchmark)
open class IndexOfByteStringBenchmark : IndexOfByteStringBase() {
    @Benchmark
    fun benchmark() = byteString.indexOf(targetByteString)
}

@State(Scope.Benchmark)
open class LastIndexOfByteStringBenchmark : IndexOfByteStringBase() {
    @Benchmark
    fun benchmark() = byteString.lastIndexOf(targetByteString)
}

@State(Scope.Benchmark)
abstract class IndexOfByteStringWithRepeatedMismatchBase {
    @Param("128:8:2", "128:8:7")
    var params: String = "<string length>:<pattern length>:<mismatch stride>"

    protected var byteString = ByteString()

    protected var targetByteString = ByteString()

    @Setup
    fun setup() {
        val paramsParsed = params.split(':').map { it.toInt() }.toIntArray()
        require(paramsParsed.size == 3)
        val size = paramsParsed[0]
        val patternLength = paramsParsed[1]
        val stride = paramsParsed[2]
        require(size > 0)
        require(patternLength > 0)
        require(stride in 1 until patternLength)

        val data = ByteArray(size) { TARGET_BYTE }
        val pattern = ByteArray(patternLength) { TARGET_BYTE }
        for (idx in data.indices) {
            if (idx % stride == 0) {
                data[idx] = 0
            }
        }
        byteString = ByteString(data)
        targetByteString = ByteString(pattern)
    }
}

@State(Scope.Benchmark)
open class IndexOfByteStringWithRepeatedMismatch : IndexOfByteStringWithRepeatedMismatchBase() {
    @Benchmark
    fun benchmark() = byteString.indexOf(targetByteString)
}

@State(Scope.Benchmark)
open class LastIndexOfByteStringWithRepeatedMismatch : IndexOfByteStringWithRepeatedMismatchBase() {
    @Benchmark
    fun benchmark() = byteString.lastIndexOf(targetByteString)
}

@State(Scope.Benchmark)

abstract class StartsWithBenchmarkBase {
    protected abstract fun getRawParams(): String

    protected var byteString = ByteString()

    protected var targetByteString = ByteString()

    @Setup
    fun setup() {
        val paramsParsed = getRawParams().split(':').map { it.toInt() }.toIntArray()
        require(paramsParsed.size == 3)
        val size = paramsParsed[0]
        val patternLength = paramsParsed[1]
        val mismatchOffset = paramsParsed[2]
        require(size > 0)
        require(patternLength > 0)
        require(mismatchOffset == -1 || mismatchOffset in (0 until size))

        val data = ByteArray(size)
        val prefix = ByteArray(patternLength)
        if (mismatchOffset != -1) {
            data[mismatchOffset] = TARGET_BYTE
        }
        byteString = ByteString(data)
        targetByteString = ByteString(prefix)
    }
}

@State(Scope.Benchmark)
open class StartsWithBenchmark : StartsWithBenchmarkBase() {
    @Param("128:8:-1", "128:8:0", "128:8:7")
    var params: String = "<string length>:<prefix/suffix length>:<mismatch offset, or -1>"

    override fun getRawParams(): String = params

    @Benchmark
    fun benchmark() = byteString.startsWith(targetByteString)
}

@State(Scope.Benchmark)
open class EndsWithBenchmark : StartsWithBenchmarkBase() {
    @Param("128:8:-1", "128:8:127", "128:8:120")
    var params: String = "<string length>:<prefix/suffix length>:<mismatch offset, or -1>"

    override fun getRawParams(): String = params

    @Benchmark
    fun benchmark() = byteString.endsWith(targetByteString)
}

@State(Scope.Benchmark)

abstract class ByteStringComparisonBenchmarkBase {
    @Param("128")
    var length: Int = 0

    @Param("-1", "63")
    var mismatchOffset = 0

    protected var stringA = ByteString()
    protected var stringB = ByteString()

    @Setup
    fun setup() {
        require(length > 0)
        require(mismatchOffset == -1 || mismatchOffset in 0 until length)

        stringA = ByteString(ByteArray(length))
        stringB = ByteString(ByteArray(length).apply {
            if (mismatchOffset != -1) {
                this[mismatchOffset] = TARGET_BYTE
            }
        })
    }
}

@State(Scope.Benchmark)
open class CompareBenchmark : ByteStringComparisonBenchmarkBase() {
    @Benchmark
    fun benchmark() = stringA.compareTo(stringB)
}

@State(Scope.Benchmark)
open class EqualsBenchmark : ByteStringComparisonBenchmarkBase() {
    @Param("true", "false")
    var useHashCode: Boolean = false

    @Setup
    fun computeHashCodes() {
        if (useHashCode) {
            stringA.hashCode()
            stringB.hashCode()
        }
    }

    @Benchmark
    fun benchmark() = stringA == stringB
}

@State(Scope.Benchmark)
open class ByteStringHashCode {
    @Param("8", "128")
    var size: Int = 0

    @Param("true", "false")
    var recomputeOnEveryCall: Boolean = false

    private var byteString = ByteString()

    @Setup
    fun setupByteString() {
        require(size > 0) { "Invalid byte string size: $size" }
        val ba = ByteArray(size)
        if (recomputeOnEveryCall) {
            ba[0] = -31
            check(ba.contentHashCode() == 0) { "Hash code is non zero" }
        } else {
            check(ba.contentHashCode() != 0) { "Hash code is zero" }
        }


        byteString = ByteString(ba)
    }

    @Benchmark
    fun benchmark(): Int = byteString.hashCode()
}
