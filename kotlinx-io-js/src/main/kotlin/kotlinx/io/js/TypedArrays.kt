package kotlinx.io.js

import kotlinx.io.core.*
import org.khronos.webgl.*

fun ByteReadPacket.readAvailable(dst: Int8Array, offset: Int = 0, length: Int = dst.byteLength - offset): Int {
    var read = 0
    var rem = minOf(length, remaining)

    while (rem > 0) {
        @Suppress("INVISIBLE_MEMBER")
        val bb: BufferView = prepareRead(1) ?: break
        val size = minOf(rem, bb.readRemaining)
        bb.read(dst, offset + read, size)
        read += size
        rem -= size
        if (bb.readRemaining == 0) {
            @Suppress("INVISIBLE_MEMBER")
            releaseHead(bb)
        }
    }

    return read
}

fun ByteReadPacket.readAvailable(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength - offset): Int {
    return readAvailable(Int8Array(dst), offset, length)
}

fun ByteReadPacket.readFully(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength - offset) {
    return readFully(Int8Array(dst), offset, length)
}

fun ByteReadPacket.readFully(dst: Int8Array, offset: Int = 0, length: Int = dst.length - offset) {
    require(length <= remaining)
    readAvailable(dst, offset, length)
}

fun ByteReadPacket.readArrayBuffer(n: Int = remaining): ArrayBuffer {
    val buffer = ArrayBuffer(n)
    readFully(buffer)
    return buffer
}

fun BytePacketBuilder.writeFully(src: ArrayBuffer, offset: Int = 0, length: Int = src.byteLength - offset) {
    writeFully(Int8Array(src), offset, length)
}

fun BytePacketBuilder.writeFully(src: Int8Array, offset: Int = 0, length: Int = src.length - offset) {
    var written = 0
    var rem = length

    while (rem > 0) {
        @Suppress("INVISIBLE_MEMBER")
        write(1) { bb: BufferView ->
            val size = minOf(bb.writeRemaining, rem)
            bb.write(src, written + offset, size)
            written += size
            rem -= size
            size
        }
    }
}
