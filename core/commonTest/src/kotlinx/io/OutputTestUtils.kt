package kotlinx.io

import kotlinx.io.*
import kotlinx.io.buffer.*

class LambdaOutput(private val block: (source: Buffer, startIndex: Int, endIndex: Int) -> Unit) : Output() {
    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
        block(source, startIndex, endIndex)
    }

    override fun closeSource() {}
}