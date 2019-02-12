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
# Results on unit 660
# Run complete. Total time: 00:05:04

Benchmark                                  Mode  Cnt      Score     Error   Units
TextDecodeBenchmark.largeASCIIKt          thrpt   15     28,425 ±   1,776  ops/ms
TextDecodeBenchmark.largeASCIIReader      thrpt   15     17,521 ±   0,638  ops/ms
TextDecodeBenchmark.largeASCIIStringCtor  thrpt   15     50,847 ±   1,266  ops/ms
TextDecodeBenchmark.largeMbKt             thrpt   15      8,586 ±   0,286  ops/ms
TextDecodeBenchmark.largeMbReader         thrpt   15      5,752 ±   0,058  ops/ms
TextDecodeBenchmark.largeMbStringCtor     thrpt   15      7,485 ±   0,154  ops/ms
TextDecodeBenchmark.smallASCIIKt          thrpt   15   9708,667 ± 132,044  ops/ms
TextDecodeBenchmark.smallASCIIReader      thrpt   15    583,049 ±   9,436  ops/ms
TextDecodeBenchmark.smallASCIIStringCtor  thrpt   15  25704,755 ± 148,262  ops/ms
TextDecodeBenchmark.smallMbKt             thrpt   15   8374,509 ± 108,271  ops/ms
TextDecodeBenchmark.smallMbReader         thrpt   15    581,390 ±   6,250  ops/ms
TextDecodeBenchmark.smallMbStringCtor     thrpt   15  18280,165 ± 167,847  ops/ms
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
