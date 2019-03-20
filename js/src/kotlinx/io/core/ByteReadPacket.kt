@file:Suppress("FunctionName")

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
            @Suppress("DEPRECATION")
            return IoBuffer(Memory.of(sub), null)
        }

        override fun disposeInstance(instance: ChunkBuffer) {@Suppress("DEPRECATION")
            check(instance is IoBuffer) { "Only IoBuffer could be recycled" }
            block(array)
        }
    }

    return ByteReadPacket(pool.borrow().apply { resetForRead() }, pool)
}
