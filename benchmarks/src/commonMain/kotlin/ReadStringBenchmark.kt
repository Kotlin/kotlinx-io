package kotlinx.io.benchmarks

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readCodePointValue
import kotlinx.io.readString
import kotlinx.io.writeCodePointValue
import kotlinx.io.writeString
import kotlin.random.Random


@State(Scope.Benchmark)
open class ReadStringBenchmark() {

    @Param("16", "64", "512")
    var size: Int = 0

    val buffer: Buffer = Buffer()

    @Setup
    fun setup() {
        val string = buildString { repeat(size) { append(('a'..'z').random()) } }
        buffer.writeString(string)
    }


    @Benchmark
    fun bufferReadString(): String {
        return buffer.copy().readString()
    }

    @Benchmark
    fun sourceReadString(): String {
        val source: Source = buffer.copy()
        return source.readString()
    }
}
