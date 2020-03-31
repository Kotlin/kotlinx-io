package kotlinx.io

import kotlinx.io.buffer.Buffer
import kotlinx.io.pool.ObjectPool

/**
 * An input that can track its current reader position relative to input start
 */
abstract class CountingInput(pool: ObjectPool<Buffer>) : Input(pool) {
    private var bufferIndex: Int = 0
    private var lastBufferSize: Int = 0

    final override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        bufferIndex += lastBufferSize
        lastBufferSize = fillCounting(buffer, startIndex, endIndex, bufferIndex)
        return lastBufferSize
    }

    /**
     * Fill buffer appending counter
     * @param absoluteBufferIndex is the absolute start index of buffer start in the input
     */
    abstract fun fillCounting(buffer: Buffer, startIndex: Int, endIndex: Int, absoluteBufferIndex: Int): Int

    /**
     * Absolute read position relative to input start or last reset position
     */
    fun absolutePosition(): Int = bufferIndex + bufferPosition()

    /**
     * Reset counter and read position if it is applicable
     */
    protected open fun position(startIndex: Int) {
        bufferIndex = startIndex
        lastBufferSize = 0
        //TODO discard current buffer to avoid misuse
    }
}