package kotlinx.io.core

@SharedImmutable
actual val PACKET_MAX_COPY_SIZE: Int = 200

@SharedImmutable
internal const val BUFFER_VIEW_POOL_SIZE = 1024

@SharedImmutable
internal const val BUFFER_VIEW_SIZE = 4096

actual fun BytePacketBuilder(headerSizeHint: Int) = BytePacketBuilder(headerSizeHint, IoBuffer.Pool)

actual typealias EOFException = kotlinx.io.errors.EOFException
