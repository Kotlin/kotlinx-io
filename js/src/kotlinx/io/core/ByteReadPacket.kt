package kotlinx.io.core

import kotlinx.io.bits.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import org.khronos.webgl.*

actual fun ByteReadPacket(array: ByteArray, offset: Int, length: Int, block: (ByteArray) -> Unit): ByteReadPacket {
    val content = array.asDynamic() as Int8Array
    val sub = when {
        offset == 0 && length == array.size -> content.buffer
        else -> content.buffer.slice(offset, offset + length)
    }

    val pool = object : SingleInstancePool<ChunkBuffer>() {
        override fun produceInstance(): ChunkBuffer {
            return ChunkBuffer(Memory.of(sub), null)
        }

        override fun disposeInstance(instance: ChunkBuffer) {
            block(array)
        }
    }

    return ByteReadPacket(pool.borrow().apply { resetForRead() }, pool)
}
