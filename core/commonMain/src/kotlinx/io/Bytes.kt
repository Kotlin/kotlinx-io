package kotlinx.io

import kotlinx.io.memory.*

typealias BytesPointer = Int

class Bytes {
    private val pages: MutableList<Memory> = mutableListOf()
    private val limits: MutableList<Int> = mutableListOf()

    internal fun append(page: Memory, limit: Int) {
        pages.add(page)
        limits.add(limit)
    }

    internal fun discardFirst() {
        pages.removeAt(0)
        limits.removeAt(0)
    }

    inline internal fun pointed(pointer: BytesPointer, consumer: (Memory, Int) -> Unit) =
        consumer(pages[pointer], limits[pointer])

    inline internal fun advancePointer(pointer: BytesPointer): BytesPointer =
        pointer + 1

    inline internal fun isEmpty() =
        pages.isEmpty()

    inline internal fun isAfterLast(index: BytesPointer) =
        index >= pages.size

    fun asInput(): Input {
        return object: Input(this) {
            override fun close() {}
            override fun fill(destination: Memory, offset: Int, length: Int): Int = 0
        }
    }

    override fun toString() = "Bytes(${pages.size} pages)"

    companion object {
        const val InvalidPointer = Int.MIN_VALUE
        const val StartPointer = 0
    }

}

fun buildBytes(builder: Output.()->Unit) : Bytes {
    return BytesOutput().apply(builder).bytes()
}

class BytesOutput : Output() {
    private val bytes = Bytes()

    fun bytes(): Bytes {
        close()
        return bytes
    }
    
    override fun flush(source: Memory, length: Int): Int {
        bytes.append(source, length)
        return length
    }

    override fun close() {
        flush()
    }
}