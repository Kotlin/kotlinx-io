package kotlinx.io.js

import kotlinx.io.core.*
import org.khronos.webgl.*

/**
 * Read at most [length] bytes to the specified [dst] typed array at optional [offset]
 * @return number of copied bytes
 */
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

/**
 * Read at most [length] bytes to the specified [dst] array buffer at optional [offset]
 * @return number of copied bytes
 */
fun ByteReadPacket.readAvailable(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength - offset): Int {
    return readAvailable(Int8Array(dst), offset, length)
}

/**
 * Read exactly [length] bytes to the specified [dst] array buffer at optional [offset]
 */
fun ByteReadPacket.readFully(dst: ArrayBuffer, offset: Int = 0, length: Int = dst.byteLength - offset) {
    return readFully(Int8Array(dst), offset, length)
}

/**
 * Read exactly [length] bytes to the specified [dst] typed array at optional [offset]
 */
fun ByteReadPacket.readFully(dst: Int8Array, offset: Int = 0, length: Int = dst.length - offset) {
    require(length <= remaining)
    readAvailable(dst, offset, length)
}

/**
 * Read exactly [n] bytes to a new array buffer instance
 */
fun ByteReadPacket.readArrayBuffer(n: Int = remaining): ArrayBuffer {
    val buffer = ArrayBuffer(n)
    readFully(buffer)
    return buffer
}

/**
 * Write exactly [length] bytes from the specified [src] array buffer
 */
fun BytePacketBuilder.writeFully(src: ArrayBuffer, offset: Int = 0, length: Int = src.byteLength - offset) {
    writeFully(Int8Array(src), offset, length)
}

/**
 * Write exactly [length] bytes from the specified [src] typed array
 */
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
