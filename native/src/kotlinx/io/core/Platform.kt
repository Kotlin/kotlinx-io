package kotlinx.io.core

actual val PACKET_MAX_COPY_SIZE: Int = 200
internal const val BUFFER_VIEW_POOL_SIZE = 1024
internal const val BUFFER_VIEW_SIZE = 4096

actual fun BytePacketBuilder(headerSizeHint: Int) = BytePacketBuilder(headerSizeHint, IoBuffer.Pool)

actual class EOFException actual constructor(message: String) : Exception(message)
