@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package kotlinx.io.streams

import kotlinx.cinterop.*
import kotlinx.io.bits.Memory
import kotlinx.io.core.*
import kotlinx.io.internal.utils.*
import platform.posix.*

@ExperimentalIoApi
inline fun <R> CPointer<FILE>.use(block: (CPointer<FILE>) -> R): R {
    return try {
        block(this)
    } finally {
        fclose(this) // TODO handle errors
    }
}

@ExperimentalIoApi
fun fwrite(buffer: Buffer, stream: CPointer<FILE>): size_t {
    var written: size_t = 0u

    buffer.readDirect { pointer ->
        val result = fwrite(pointer, 1.convert(), buffer.readRemaining.convert(), stream)
        written = result

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        result.convert()
    }

    return written
}

@ExperimentalIoApi
fun write(fildes: Int, buffer: Buffer): ssize_t {
    var written: ssize_t = 0

    buffer.readDirect { pointer ->
        val result = write(fildes, pointer, buffer.readRemaining.convert())
        written = result.convert()

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        // however the returned value could be -1 so clamp it
        result.convert<Int>().coerceAtLeast(0)
    }

    return written
}

@ExperimentalIoApi
fun send(socket: KX_SOCKET, buffer: Buffer, flags: Int): ssize_t {
    var written: ssize_t = 0

    buffer.readDirect { pointer ->
        val result = send(socket, pointer, buffer.readRemaining.convert(), flags)
        written = result.convert()

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        // however the returned value could be -1 so clamp it
        result.convert<Int>().coerceAtLeast(0)
    }

    return written
}

@ExperimentalIoApi
fun fread(buffer: Buffer, stream: CPointer<FILE>): size_t {
    var bytesRead: size_t = 0u

    buffer.writeDirect { pointer ->
        val result = fread(pointer, 1.convert(), buffer.writeRemaining.convert(), stream)
        bytesRead = result

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        result.convert()
    }

    return bytesRead
}

@ExperimentalIoApi
fun fread(destination: Memory, offset: Int, length: Int, stream: CPointer<FILE>): Int {
    return fread(destination, offset.toLong(), length.toLong(), stream).toInt()
}

@ExperimentalIoApi
fun fread(destination: Memory, offset: Long, length: Long, stream: CPointer<FILE>): Long {
    val maxLength = minOf(length, Int.MAX_VALUE.toLong(), size_t.MAX_VALUE.toLong())
    val pointer = destination.pointer + offset

    val result = fread(pointer, 1.convert(), maxLength.convert(), stream)

    // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
    return result.convert()
}


@ExperimentalIoApi
fun read(fildes: Int, buffer: Buffer): ssize_t {
    var bytesRead: ssize_t = 0

    buffer.writeDirect { pointer ->
        val size = minOf(
            ssize_t.MAX_VALUE.toULong(),
            SSIZE_MAX.toULong(),
            buffer.writeRemaining.toULong()
        ).convert<size_t>()

        val result = read(fildes, pointer, size.convert())
        bytesRead = result.convert()

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        // however the returned value could be -1 so clamp it
        result.convert<Int>().coerceAtLeast(0)
    }

    return bytesRead
}

@ExperimentalIoApi
fun read(fildes: Int, destination: Memory, offset: Int, length: Int): Int {
    return read(fildes, destination, offset.toLong(), length.toLong()).toInt()
}

@ExperimentalIoApi
fun read(fildes: Int, destination: Memory, offset: Long, length: Long): Long {

    val maxLength = minOf<Long>(
        ssize_t.MAX_VALUE.convert(),
        length
    )

    return read(fildes, destination.pointer + offset, maxLength.convert()).convert<Long>().coerceAtLeast(0)
}

@ExperimentalIoApi
fun recv(socket: KX_SOCKET, buffer: Buffer, flags: Int): ssize_t {
    var bytesRead: ssize_t = 0

    buffer.writeDirect { pointer ->
        val result = recv(socket, pointer, buffer.writeRemaining.convert(), flags)
        bytesRead = result.convert()

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        // however the returned value could be -1 so clamp it
        result.convert<Int>().coerceAtLeast(0)
    }

    return bytesRead
}

@ExperimentalIoApi
fun recvfrom(
    socket: KX_SOCKET,
    buffer: Buffer,
    flags: Int,
    addr: CValuesRef<sockaddr>,
    addr_len: CValuesRef<KX_SOCKADDR_LENVar>
): ssize_t {
    var bytesRead: ssize_t = 0

    buffer.writeDirect { pointer ->
        val result = recvfrom(socket, pointer, buffer.writeRemaining.convert(), flags, addr, addr_len)
        bytesRead = result.convert()

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        // however the returned value could be -1 so clamp it
        result.convert<Int>().coerceAtLeast(0)
    }

    return bytesRead
}

@ExperimentalIoApi
fun sendto(
    socket: KX_SOCKET, buffer: Buffer, flags: Int,
    addr: CValuesRef<sockaddr>,
    addr_len: KX_SOCKADDR_LEN
): ssize_t {
    var written: ssize_t = 0

    buffer.readDirect { pointer ->
        val result = sendto(socket, pointer, buffer.readRemaining.convert(), flags, addr, addr_len)
        written = result.convert()

        // it is completely safe to convert since the returned value will be never greater than Int.MAX_VALUE
        // however the returned value could be -1 so clamp it
        result.convert<Int>().coerceAtLeast(0)
    }

    return written
}
