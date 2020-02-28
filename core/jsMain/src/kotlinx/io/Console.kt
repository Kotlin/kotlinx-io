package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.text.*

private val isNodeJs = js(
    "typeof process !== 'undefined' && process.versions != null && process.versions.node != null"
) as Boolean

public actual object Console {
    public actual val input: Input
        get() {
            throw NotImplementedError("Standard input is not implemented in JS")
        }

    public actual val output: Output = ConsoleOut
    public actual val error: Output = ConsoleErr
}

private object ConsoleOut : ConsolePrinter({ console.log(it) }, "output")
private object ConsoleErr : ConsolePrinter({ console.error(it) }, "error")

private open class ConsolePrinter(private val printer: (String) -> Unit, private val name: String) : Output() {
    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
        val buffer = source.view.buffer.slice(startIndex, endIndex)
        val decoder = if (isNodeJs) {
            js("new (require('util').TextDecoder)('utf-8')")
        } else {
            TextDecoder("utf-8")
        }
        printer(decoder.decode(buffer))
    }

    override fun closeSource() {
        throw IllegalStateException("Console.$name cannot be closed")
    }
}