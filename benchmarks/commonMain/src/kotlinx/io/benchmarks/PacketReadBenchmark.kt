package kotlinx.io.benchmarks

import kotlinx.io.core.*
import org.jetbrains.gradle.benchmarks.*
import kotlin.random.*

@State(Scope.Benchmark)
class PacketReadBenchmark {
    private final val size = 32 * 1024 * 1024
    private final val array = ByteArray(size)
    private final val packet: ByteReadPacket

    init {
        Random.nextBytes(array)
        packet = BytePacketBuilder().apply {
            writeFully(array)
        }.build()
    }
    
    
    @Benchmark
    fun copyAndRelease() {
        val input = packet.copy()
        input.release()
    }

    @Benchmark
    fun myInput(): Long {
        val input = packet.copy()
        var c = 0L

        repeat(size) {
            c += input.readByte().toLong() and 0xff
        }

        return c
    }

    @Benchmark
    fun myInputTakeWhile(): Long {
        val input = packet.copy()
        var c = 0L

        input.takeWhile { buffer ->
            repeat(buffer.readRemaining) {
                c += buffer.readByte().toLong() and 0xff
            }
            true
        }

        return c
    }

    @Benchmark
    fun directArrayAccess(): Long {
        val input = array
        var c = 0L

        for (i in 0 until size - 1) {
            c += input[i].toLong() and 0xff
        }

        return c
    }
    
    @Benchmark
    fun myInputReadLong(): Long {
        val input = packet.copy()
        var c = 0L

        repeat(input.remaining.toInt() / 8) {
            c += input.readLong()
        }

        return c
    }
}
