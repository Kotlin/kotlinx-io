package kotlinx.io.bytes

import kotlinx.io.*
import kotlinx.io.buffer.*

class ByteArrayInput(private val input: ByteArray) : Input() {
    private var consumed = 0

    override fun closeSource() {
    }

    override fun fill(buffer: Buffer): Int {
        if (consumed >= input.size) return 0
        val filled = buffer.size.coerceAtMost(input.size - consumed)
        repeat(filled) {
            buffer[it] = input[consumed + it]
        }
        consumed += filled
        return filled
    }
}