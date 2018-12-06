package kotlinx.io.tests.benchmarks

import kotlinx.io.core.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 15)
//@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class TextDecodeBenchmark {


    /*
# Run complete. Total time: 00:05:05

Benchmark                                  Mode  Cnt      Score     Error   Units
TextDecodeBenchmark.largeASCIIKt          thrpt   15      5,090 ±   0,186  ops/ms
TextDecodeBenchmark.largeASCIIReader      thrpt   15     17,374 ±   0,769  ops/ms
TextDecodeBenchmark.largeASCIIStringCtor  thrpt   15     49,870 ±   2,700  ops/ms
TextDecodeBenchmark.largeMbKt             thrpt   15      3,146 ±   0,066  ops/ms
TextDecodeBenchmark.largeMbReader         thrpt   15      6,137 ±   0,244  ops/ms
TextDecodeBenchmark.largeMbStringCtor     thrpt   15      7,640 ±   0,541  ops/ms
TextDecodeBenchmark.smallASCIIKt          thrpt   15  11766,753 ± 371,396  ops/ms
TextDecodeBenchmark.smallASCIIReader      thrpt   15    584,426 ±  67,464  ops/ms
TextDecodeBenchmark.smallASCIIStringCtor  thrpt   15  27157,153 ± 965,774  ops/ms
TextDecodeBenchmark.smallMbKt             thrpt   15   9256,542 ± 478,120  ops/ms
TextDecodeBenchmark.smallMbReader         thrpt   15    642,241 ±  48,872  ops/ms
TextDecodeBenchmark.smallMbStringCtor     thrpt   15  19371,117 ± 437,930  ops/ms
     */

    @Benchmark
    fun smallMbKt() = smallTextPacket.copy().readText()

    @Benchmark
    fun smallMbReader() = smallTextBytes.inputStream().reader().readText()

    @Benchmark
    fun smallMbStringCtor() = String(smallTextBytes, Charsets.UTF_8)

    @Benchmark
    fun largeMbKt() = largeTextPacket.copy().readText()

    @Benchmark
    fun largeMbReader() = largeTextBytes.inputStream().reader().readText()

    @Benchmark
    fun largeMbStringCtor() = String(largeTextBytes, Charsets.UTF_8)

    @Benchmark
    fun smallASCIIKt() = smallTextPacketASCII.copy().readText()

    @Benchmark
    fun smallASCIIReader() = smallTextBytesASCII.inputStream().reader().readText()

    @Benchmark
    fun smallASCIIStringCtor() = String(smallTextBytesASCII, Charsets.UTF_8)

    @Benchmark
    fun largeASCIIKt() = largeTextPacketASCII.copy().readText()

    @Benchmark
    fun largeASCIIReader() = largeTextBytesASCII.inputStream().reader().readText()

    @Benchmark
    fun largeASCIIStringCtor() = String(largeTextBytesASCII, Charsets.UTF_8)

    companion object {
        private val smallTextBytes = "\u0422\u0432\u0437.".toByteArray(Charsets.UTF_8)
        private val smallTextBytesASCII = "ABC.".toByteArray(Charsets.UTF_8)
        private val largeTextBytes = ByteArray(smallTextBytes.size * 10000) {
            smallTextBytes[it % smallTextBytes.size]
        }
        private val largeTextBytesASCII = ByteArray(smallTextBytesASCII.size * 10000) {
            smallTextBytesASCII[it % smallTextBytesASCII.size]
        }

        private val smallTextPacket = buildPacket { writeFully(smallTextBytes) }
        private val smallTextPacketASCII = buildPacket { writeFully(smallTextBytesASCII) }
        private val largeTextPacket = buildPacket { writeFully(largeTextBytes) }
        private val largeTextPacketASCII = buildPacket { writeFully(largeTextBytesASCII) }
    }

}