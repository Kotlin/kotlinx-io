package kotlinx.io.core

import kotlinx.io.core.internal.*

actual val PACKET_MAX_COPY_SIZE: Int = 200
internal const val BUFFER_VIEW_POOL_SIZE = 100
internal const val BUFFER_VIEW_SIZE = 4096

actual fun BytePacketBuilder(headerSizeHint: Int) = BytePacketBuilder(headerSizeHint, ChunkBuffer.Pool)

actual typealias EOFException = kotlinx.io.errors.EOFException

