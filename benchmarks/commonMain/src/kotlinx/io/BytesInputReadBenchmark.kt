package kotlinx.io

import kotlinx.benchmark.*
import kotlinx.io.bytes.*

/**
 * Benchmark with prebuilt ByteInput
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
class BytesInputReadBenchmark {
    lateinit var input: Input

    @Param("2097152")
    open var size: Int = 42

    @Setup
    fun setup() {
        val size = size
        input = buildInput {
            writeByteArray(ByteArray(size) { 42 })
        }
    }

    // Discarding
    @Benchmark
    fun readLastByte(): Byte = with(input) {
        discard(size - 1)
        readByte()
    }

    @Benchmark
    fun readFirstByte(): Byte = input.readByte()

    @Benchmark
    fun readMiddleByte(): Byte = with(input) {
        discard(size / 2)
        readByte()
    }

    @Benchmark
    fun readByteArray(): ByteArray = input.readByteArray()

    // In preview
    @Benchmark
    fun readLastByteInPreview(): Byte = input.preview {
        discard(size - 1)
        readByte()
    }

    @Benchmark
    fun readFirstByteInPreview(): Byte = input.preview {
        readByte()
    }

    @Benchmark
    fun readMiddleByteInPreview(): Byte = input.preview {
        discard(size / 2)
        readByte()
    }

    @Benchmark
    fun readByteArrayInPreview(): ByteArray = input.preview {
        readByteArray()
    }
}