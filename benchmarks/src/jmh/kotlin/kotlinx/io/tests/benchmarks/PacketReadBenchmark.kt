package kotlinx.io.tests.benchmarks

import kotlinx.io.core.*
import org.openjdk.jmh.annotations.*
import java.io.*
import java.io.EOFException
import java.nio.*
import java.util.*
import java.util.concurrent.*

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 10)
@Measurement(iterations = 15)
//@BenchmarkMode(Mode.Throughput, Mode.AverageTime)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class PacketReadBenchmark {
    private final val size = 32 * 1024 * 1024
    private final val array = ByteArray(size)
    private final val packet: ByteReadPacket
    private final val directBuffer = ByteBuffer.allocateDirect(size)!!

    init {
        Random().nextBytes(array)
        packet = BytePacketBuilder().apply {
            writeFully(array)
        }.build()
        directBuffer.put(array)
        directBuffer.clear()
    }

    @Benchmark
    fun bufferInputStream(): Long {
        val stream = BufferedInputStream(ByteArrayInputStream(array))
        var c = 0L

        while (true) {
            val rc = stream.read()
            if (rc == -1) break
            c += rc.toLong() and 0xff
        }

        return c
    }

    @Benchmark
    fun byteArrayInputStream(): Long {
        val stream = ByteArrayInputStream(array)
        var c = 0L

        while (true) {
            val rc = stream.read()
            if (rc == -1) break
            c += rc.toLong() and 0xff
        }

        return c
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
    fun myInputForEach(): Long {
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
    fun directBuffer(): Long {
        val input = directBuffer.duplicate()
        var c = 0L
        while (input.hasRemaining()) {
            c += input.get().toLong() and 0xff
        }

        return c
    }

    @Benchmark
    fun dataInputStreamReadLong(): Long {
        val input = DataInputStream(ByteArrayInputStream(array))
        var c = 0L

        repeat(size / 8) {
            c += input.readLong()
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