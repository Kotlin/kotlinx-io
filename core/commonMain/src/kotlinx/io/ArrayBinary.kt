package kotlinx.io

import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.set
import kotlin.math.min

class ByteArrayInput(
    private val input: ByteArray,
    val offset: BinarySize = 0,
    val size: BinarySize = input.size - offset
) : Input() {
    private var consumed = 0

    init {
        if (offset + size > input.size) error("Can't read byte ${offset + size} from array with size ${input.size}")
    }

    override fun closeSource() {
    }

    override fun fill(buffer: Buffer): Int {
        if (consumed >= size) return 0
        val filled = buffer.size.coerceAtMost(size - consumed)
        repeat(filled) {
            buffer[it] = input[offset + consumed + it]
        }
        consumed += filled
        return filled
    }
}

/**
 * A binary wrapping existing array
 */
@ExperimentalIoApi
inline class ArrayBinary(val array: ByteArray) : RandomAccessBinary {
    override val size: BinarySize get() = array.size

    override fun <R> read(from: BinarySize, atMost: BinarySize, block: Input.() -> R): R {
        return ByteArrayInput(array, from, min(atMost, array.size - from)).use(block)
    }
}

@ExperimentalIoApi
fun ByteArray.asBinary() = ArrayBinary(this)