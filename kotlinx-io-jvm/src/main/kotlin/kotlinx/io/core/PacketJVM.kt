package kotlinx.io.core

actual val PACKET_MAX_COPY_SIZE: Int = 500

fun BytePacketBuilder() = BytePacketBuilder(0)
actual fun BytePacketBuilder(headerSizeHint: Int): BytePacketBuilder = BytePacketBuilder(headerSizeHint, BufferView.Pool)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias EOFException = java.io.EOFException
