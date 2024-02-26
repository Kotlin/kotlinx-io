package kotlinx.io.benchmarks

import kotlinx.io.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*
import kotlin.random.*


@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class BulkWriteBenchmark {

    private val data = LongArray(1024 * 128) { Random.nextLong() }
    private val buffer = Buffer().also {
        for (long in data) {
            it.writeLong(long)
        } }


    @Benchmark
    fun write(): Buffer {
        val buffer = Buffer()
        for (long in data) {
            buffer.writeLong(long)
        }
        return buffer
    }

    @Benchmark
    fun writeVh(): Buffer {
        val buffer = Buffer()
        for (long in data) {
            buffer.writeLongVh(long)
        }
        return buffer
    }

    @Benchmark
    fun readLongArray(): LongArray {
        val copy = buffer.copy()
        for (index in data.indices) {
            data[index] = copy.readLong()
        }
        return data
    }

    @Benchmark
    fun readLongArrayVh(): LongArray {
        val copy = buffer.copy()
        for (index in data.indices) {
            data[index] = copy.readLongVh()
        }
        return data
    }
}
