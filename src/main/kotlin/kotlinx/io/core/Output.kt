package kotlinx.io.core

import kotlinx.io.core.internal.*

/**
 * This shouldn't be implemented directly. Inherit [AbstractOutput] instead.
 */
expect interface Output : Appendable, Closeable {
    var byteOrder: ByteOrder

    fun writeByte(v: Byte)
    fun writeShort(v: Short)
    fun writeInt(v: Int)
    fun writeLong(v: Long)
    fun writeFloat(v: Float)
    fun writeDouble(v: Double)

    fun writeFully(src: ByteArray, offset: Int, length: Int)
    fun writeFully(src: ShortArray, offset: Int, length: Int)
    fun writeFully(src: IntArray, offset: Int, length: Int)
    fun writeFully(src: LongArray, offset: Int, length: Int)
    fun writeFully(src: FloatArray, offset: Int, length: Int)
    fun writeFully(src: DoubleArray, offset: Int, length: Int)
    fun writeFully(src: IoBuffer, length: Int)

    fun append(csq: CharArray, start: Int, end: Int): Appendable

    fun fill(n: Long, v: Byte)

    fun flush()
    override fun close()

    @DangerousInternalIoApi
    fun `$prepareWrite$`(n: Int): IoBuffer

    @DangerousInternalIoApi
    fun `$afterWrite$`()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.append(csq: CharSequence, start: Int = 0, end: Int = csq.length): Appendable {
    return append(csq, start, end)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.append(csq: CharArray, start: Int = 0, end: Int = csq.size): Appendable {
    return append(csq, start, end)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: ByteArray, offset: Int = 0, length: Int = src.size - offset) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: ShortArray, offset: Int = 0, length: Int = src.size - offset) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: IntArray, offset: Int = 0, length: Int = src.size - offset) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: LongArray, offset: Int = 0, length: Int = src.size - offset) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: FloatArray, offset: Int = 0, length: Int = src.size - offset) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: DoubleArray, offset: Int = 0, length: Int = src.size - offset) {
    writeFully(src, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.writeFully(src: IoBuffer, length: Int = src.readRemaining) {
    writeFully(src, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Output.fill(n: Long, v: Byte = 0) {
    fill(n, v)
}

/**
 * Append number of chunks invoking [block] function while the returned value is true.
 * Depending on the output underlying implementation it could invoke [block] function with the same buffer several times
 * however it is guaranteed that it is always non-empty.
 */
inline fun Output.writeWhile(block: (IoBuffer) -> Boolean) {
    try {
        var tail = @Suppress("DEPRECATION_ERROR") `$prepareWrite$`(1)

        while (true) {
            if (!block(tail)) break
            tail = @Suppress("DEPRECATION_ERROR") `$prepareWrite$`(1)
        }
    } finally {
        @Suppress("DEPRECATION_ERROR") `$afterWrite$`()
    }
}

/**
 * Append number of chunks invoking [block] function while the returned value is positive.
 * If returned value is positive then it will be invoked again with a buffer having at least requested number of
 * bytes space (could be the same buffer as before if it complies to the restriction).
 * @param initialSize for the first buffer passed to [block] function
 */
inline fun Output.writeWhileSize(initialSize: Int = 1, block: (IoBuffer) -> Int) {
    try {
        var tail = @Suppress("DEPRECATION_ERROR") `$prepareWrite$`(initialSize)

        var size: Int
        while (true) {
            size = block(tail)
            if (size <= 0) break
            tail = @Suppress("DEPRECATION_ERROR") `$prepareWrite$`(size)
        }
    } finally {
        @Suppress("DEPRECATION_ERROR") `$afterWrite$`()
    }
}

fun Output.writePacket(packet: ByteReadPacket) {
    if (this is BytePacketBuilderBase) {
        writePacket(packet)
        return
    }

    packet.takeWhile { from ->
        writeFully(from)
        true
    }
}
