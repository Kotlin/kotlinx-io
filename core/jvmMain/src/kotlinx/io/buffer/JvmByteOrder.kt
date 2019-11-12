@file:JvmName("ByteOrderJVMKt")

package kotlinx.io.buffer

public actual enum class ByteOrder  {
    BIG_ENDIAN,
    LITTLE_ENDIAN;

    public actual companion object {
        public actual val native: ByteOrder = orderOf(java.nio.ByteOrder.nativeOrder())
    }
}

private fun orderOf(nioOrder: java.nio.ByteOrder): ByteOrder = when (nioOrder) {
    java.nio.ByteOrder.BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
    else -> ByteOrder.LITTLE_ENDIAN
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Long.reverseByteOrder(): Long = java.lang.Long.reverseBytes(this)
