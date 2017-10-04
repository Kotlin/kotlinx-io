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
