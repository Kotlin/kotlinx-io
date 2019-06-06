package kotlinx.io.benchmarks

import kotlinx.io.*
import kotlinx.io.buffer.*
import org.jetbrains.gradle.benchmarks.*
import kotlin.random.*

@State(Scope.Benchmark)
class InputReadingBenchmark {
    val bufferSize = 1024

    private fun sequentialInfiniteInput(): Input {
        return object : Input() {
            private var value = 0L
            private var sliceRandom = Random(bufferSize)

            override fun closeSource() {}

            override fun fill(destination: Buffer, offset: Int, length: Int): Int {
                // Simulate different slices being read, not just length
                val readLength = minOf(sliceRandom.nextInt(bufferSize), length)

                for (index in offset until offset + readLength) {
                    destination.storeByteAt(index, value++.toByte())
                }
                return readLength
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
}

/*
fun main() {
    repeat(1_000_000) {
        InputReadingBenchmark().inputReadBytes()
    }
}
*/
