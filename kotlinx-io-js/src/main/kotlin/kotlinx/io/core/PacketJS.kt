package kotlinx.io.core

import kotlinx.io.pool.*
import org.khronos.webgl.*

actual val PACKET_MAX_COPY_SIZE: Int = 200
private const val BUFFER_VIEW_POOL_SIZE = 100
private const val BUFFER_VIEW_SIZE = 4096

internal val DefaultBufferViewPool: ObjectPool<BufferView> = object: DefaultPool<BufferView>(BUFFER_VIEW_POOL_SIZE) {
    override fun produceInstance(): BufferView {
        return BufferView(ArrayBuffer(BUFFER_VIEW_SIZE), null)
    }

    override fun disposeInstance(instance: BufferView) {
        instance.unlink()
    }
}

actual fun BytePacketBuilder(headerSizeHint: Int) = BytePacketBuilder(headerSizeHint, DefaultBufferViewPool)

actual class EOFException actual constructor(message: String) : Exception(message)

fun ByteReadPacket.readAvailable(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength): Int {
    var read = 0
    var rem = minOf(length, remaining)
    val i8 = Int8Array(dst, offset, length)

    while (rem > 0) {
        @Suppress("INVISIBLE_MEMBER")
        val bb: BufferView = prepareRead(1) ?: break
        val size = minOf(rem, bb.readRemaining)
        bb.read(i8, read, size)
        read += size
        rem -= size
        if (bb.readRemaining == 0) {
            @Suppress("INVISIBLE_MEMBER")
            releaseHead(bb)
        }
    }

    return read
}