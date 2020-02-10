package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.*
import kotlinx.io.buffer.*

@State(Scope.Benchmark)
class InputReadingBenchmark {
    private fun sequentialInfiniteInput(): Input {
        return object : Input() {
            private var value = 0L

            override fun closeSource() {}

            override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
                val size = buffer.size
                for (index in startIndex until endIndex) {
                    buffer.storeByteAt(index, value++.toByte())
                }
                return size
            }
        }
    }

    @Benchmark
    fun inputReadLongs(): Long = sequentialInfiniteInput().use { input ->
        var sum = 0L
        repeat(1024) {
            sum += input.readLong()
        }
        return sum
    }

    @Benchmark
    fun inputReadDoubles(): Double = sequentialInfiniteInput().use { input ->
        var sum = 0.0
        repeat(1024) {
            sum += input.readDouble()
        }
        return sum
    }

    @Benchmark
    fun inputReadInts(): Int = sequentialInfiniteInput().use { input ->
        var sum = 0
        repeat(2048) {
            sum += input.readInt()
        }
        return sum
    }

    @Benchmark
    fun inputReadBytes(): Int = sequentialInfiniteInput().use { input ->
        var sum = 0
        repeat(8192) {
            sum += input.readByte()
        }
        return sum
    }

    @Benchmark
    fun inputPreviewLongs(): Long = sequentialInfiniteInput().use { input ->
        var sum = 0L
        input.preview {
            repeat(1024) {
                sum += input.readLong()
            }
        }

        repeat(1024) {
            sum -= input.readLong()
        }
        if (sum != 0L)
            throw Exception("Incorrect repeated read")
        return sum
    }

    @Benchmark
    fun inputReadLongsShort(): Long = sequentialInfiniteInput().use { input ->
        var sum = 0L
        repeat(64) {
            sum += input.readLong()
        }
        return sum
    }

    @Benchmark
    fun inputPreviewLongsShort(): Long = sequentialInfiniteInput().use { input ->
        var sum = 0L
        input.preview {
            repeat(64) {
                sum += input.readLong()
            }
        }

        repeat(64) {
            sum -= input.readLong()
        }
        if (sum != 0L)
            throw Exception("Incorrect repeated read")
        return sum
    }
}

fun main() {
    repeat(1_000_000) {
        InputReadingBenchmark().inputPreviewLongsShort()
    }
}
