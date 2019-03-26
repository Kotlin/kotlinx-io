package kotlinx.coroutines.io

import kotlinx.io.bits.*
import kotlinx.io.core.*

suspend inline fun ByteReadChannel.readShortLittleEndian(): Short {
    return toLittleEndian(readShort()) { reverseByteOrder() }
}

suspend inline fun ByteReadChannel.readIntLittleEndian(): Int {
    return toLittleEndian(readInt()) { reverseByteOrder() }
}

suspend inline fun ByteReadChannel.readLongLittleEndian(): Long {
    return toLittleEndian(readLong()) { reverseByteOrder() }
}

suspend inline fun ByteReadChannel.readFloatLittleEndian(): Float {
    return toLittleEndian(readFloat()) { reverseByteOrder() }
}

suspend inline fun ByteReadChannel.readDoubleLittleEndian(): Double {
    return toLittleEndian(readDouble()) { reverseByteOrder() }
}

suspend inline fun ByteWriteChannel.writeShortLittleEndian(value: Short) {
    writeShort(toLittleEndian(value) { reverseByteOrder() })
}

suspend inline fun ByteWriteChannel.writeIntLittleEndian(value: Int) {
    writeInt(toLittleEndian(value) { reverseByteOrder() })
}

suspend inline fun ByteWriteChannel.writeLongLittleEndian(value: Long) {
    writeLong(toLittleEndian(value) { reverseByteOrder() })
}

suspend inline fun ByteWriteChannel.writeFloatLittleEndian(value: Float) {
    writeFloat(toLittleEndian(value) { reverseByteOrder() })
}

suspend inline fun ByteWriteChannel.writeDoubleLittleEndian(value: Double) {
    writeDouble(toLittleEndian(value) { reverseByteOrder() })
}

@PublishedApi
@Suppress("DEPRECATION")
internal inline fun <T> ByteReadChannel.toLittleEndian(value: T, reverseBlock: T.() -> T): T {
    return when (readByteOrder) {
        ByteOrder.LITTLE_ENDIAN -> value
        else -> value.reverseBlock()
    }
}

@PublishedApi
@Suppress("DEPRECATION")
internal inline fun <T> ByteWriteChannel.toLittleEndian(value: T, reverseBlock: T.() -> T): T {
    return when (writeByteOrder) {
        ByteOrder.LITTLE_ENDIAN -> value
        else -> value.reverseBlock()
    }
}
