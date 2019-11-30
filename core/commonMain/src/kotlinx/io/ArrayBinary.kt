package kotlinx.io

import kotlin.math.min

/**
 * A binary wrapping existing array
 */
@ExperimentalIoApi
inline class ArrayBinary(val array: ByteArray) : RandomAccessBinary {
    override val size: Int get() = array.size

    override fun <R> read(from: Int, atMost: Int, block: Input.() -> R): R {
        return ByteArrayInput(
            array,
            from,
            min(from + atMost, array.size - 1)
        ).use(block)
    }
}

@ExperimentalIoApi
public fun ByteArray.asBinary(): ArrayBinary = ArrayBinary(this)

@ExperimentalIoApi
public fun <R> ByteArray.read(block: Input.() -> R): R = asBinary().read(block)