package kotlinx.io

import kotlinx.io.internal.ByteArrayInput
import kotlin.math.min

/**
 * [Binary] represents a fixed-size multi-read byte block, which is not attached to the Input which was used to create it.
 * The binary could be associated with a resource, but it should guarantee that when someone is trying to read the binary,
 * this resource is re-acquired.
 */
public interface Binary {

    val size: Int

    /**
     * Read maximum of [atMost] bytes as input from the binary, starting at [offset]. The generated input is always closed
     * when leaving scope, so it could not be leaked outside of scope of [block].
     */
    fun <R> read(offset: Int = 0, atMost: Int = size - offset, block: Input.() -> R): R
}

public class ByteArrayBinary(
    internal val array: ByteArray,
    private val start: Int = 0,
    override val size: Int = array.size - start
) : Binary {

    override fun <R> read(offset: Int, atMost: Int, block: Input.() -> R): R {
        require(offset >= 0) { "Offset must be positive" }
        require(offset < array.size) { "Offset $offset is larger than array size" }
        val input = ByteArrayInput(
            array,
            offset + start,
            min(offset + start + atMost, start + size)
        )
        return input.use(block)
    }
}

public fun ByteArray.asBinary(): Binary = ByteArrayBinary(this)

/**
 * Produce a [ByteArray] representing an exact copy of this [Binary]
 */
public fun Binary.toByteArray(): ByteArray = if (this is ByteArrayBinary) {
    array.copyOf() // TODO do we need to ensure data safety here?
} else {
    read {
        readByteArray()
    }
}

public fun Output.writeBinary(binary: Binary) {
    if (binary is ByteArrayBinary) {
        writeByteArray(binary.array)
    } else {
        binary.read {
            copyTo(this@writeBinary)
        }
    }
}