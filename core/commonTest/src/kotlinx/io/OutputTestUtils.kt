package kotlinx.io

import kotlinx.io.*
import kotlinx.io.buffer.*

class LambdaOutput(
    pool: ObjectPool<Buffer>,
    private val block: (source: Buffer, startIndex: Int, endIndex: Int) -> Boolean
) : Output(pool) {
    override fun flush(source: Buffer, startIndex: Int, endIndex: Int): Boolean = block(source, startIndex, endIndex)

    override fun closeSource() {}
}