package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.*
import kotlinx.io.text.*

private const val content = "file content with unicode 🌀 : здороваться : 여보세요 : 你好 : ñç."

@State(Scope.Benchmark)
class TextEncodingBenchmark {
    @Benchmark
    fun outputText(): Int {
        val p = buildBytes {
            writeUtf8String(content)
        }
        return p.size()
    }
    
    @Benchmark
    fun outputTextShort(): Int {
        val p = buildBytes {
            writeUtf8String(content, 0, 25)
        }
        return p.size()
    }
}
