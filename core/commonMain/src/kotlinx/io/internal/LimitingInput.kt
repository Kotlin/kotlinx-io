package kotlinx.io.internal

import kotlinx.io.*
import kotlinx.io.buffer.*
import kotlin.math.*

internal class LimitingInput(private val original: Input, private var bytesLeft: Long) : Input() {
    /*
     * Unfortunately, `fill` cannot be implemented in a zero-copy manner
     */
    override fun fill(buffer: Buffer): Int {
        if (bytesLeft <= 0L || original.eof()) return 0
        val limit = min(bytesLeft, buffer.size.toLong()).toInt() // Can always be safely cast to int
        repeat(limit) {
            if (original.eof()) return it
            buffer[it] = original.readByte()
            --bytesLeft
        }
        return limit
    }

    override fun closeSource() = original.close()
}
