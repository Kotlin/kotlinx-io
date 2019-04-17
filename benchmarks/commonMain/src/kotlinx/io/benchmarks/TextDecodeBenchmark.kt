package kotlinx.io.benchmarks

import kotlinx.io.charsets.*
import kotlinx.io.core.*
import org.jetbrains.gradle.benchmarks.*

@State(Scope.Benchmark)
class TextDecodeBenchmark {
    @Benchmark
    fun smallMbKt() = smallTextPacket.copy().readText()

    @Benchmark
    fun smallMbStringCtor() = String(smallTextBytes, charset = Charsets.UTF_8)

    @Benchmark
    fun largeMbKt() = largeTextPacket.copy().readText()

    @Benchmark
    fun largeMbStringCtor() = String(largeTextBytes, charset = Charsets.UTF_8)

    @Benchmark
    fun smallASCIIKt() = smallTextPacketASCII.copy().readText()

    @Benchmark
    fun smallASCIIStringCtor() = String(smallTextBytesASCII, charset = Charsets.UTF_8)

    @Benchmark
    fun largeASCIIKt() = largeTextPacketASCII.copy().readText()

    @Benchmark
    fun largeASCIIStringCtor() = String(largeTextBytesASCII, charset = Charsets.UTF_8)

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
