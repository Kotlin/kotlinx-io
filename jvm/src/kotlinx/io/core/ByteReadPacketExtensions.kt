package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.nio.*

actual inline fun ByteReadPacket(array: ByteArray, offset: Int, length: Int, crossinline block: (ByteArray) -> Unit): ByteReadPacket {
    return ByteReadPacket(ByteBuffer.wrap(array, offset, length)) { block(array) }
}

fun ByteReadPacket(bb: ByteBuffer, release: (ByteBuffer) -> Unit = {}): ByteReadPacket {
    val pool = poolFor(bb, release)
    val view = pool.borrow().apply { resetForRead() }
    return ByteReadPacket(view, pool)
}

private fun poolFor(bb: ByteBuffer, release: (ByteBuffer) -> Unit): ObjectPool<ChunkBuffer> {
    return SingleByteBufferPool(bb, release)
}

private class SingleByteBufferPool(val instance: ByteBuffer, val release: (ByteBuffer) -> Unit) :
    SingleInstancePool<ChunkBuffer>() {
    override fun produceInstance(): IoBuffer {
        return IoBuffer(instance)
    }

    override fun disposeInstance(instance: ChunkBuffer) {
        release(this.instance)
    }
}
