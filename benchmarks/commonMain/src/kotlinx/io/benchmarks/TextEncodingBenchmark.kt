package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.bytes.*
import kotlinx.io.text.*

private const val content = "file content with unicode 🌀 : здороваться : 여보세요 : 你好 : ñç."

@State(Scope.Benchmark)
class TextEncodingBenchmark {
    @Benchmark
    fun outputText(): Int {
        val p = buildInput {
            writeUtf8String(content)
        }
        return p.remaining
    }
    
    @Benchmark
    fun outputTextShort(): Int {
        val p = buildInput {
            writeUtf8String(content, 0, 25)
        }
        return p.remaining
    }
}
