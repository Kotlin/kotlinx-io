package kotlinx.io.streams

import kotlinx.cinterop.*
import kotlinx.io.core.*
import kotlinx.io.internal.utils.*
import platform.posix.*

private const val ZERO: size_t = 0u
private val s: KX_SOCKET = 0.convert() // do not remove! This is required to hold star import

internal fun Int.checkError(action: String = ""): Int = when {
    this < 0 -> memScoped { throw PosixException.forErrno(action = action) }
    else -> this
}

internal fun Long.checkError(action: String = ""): Long = when {
    this < 0 -> memScoped { throw PosixException.forErrno(action = action) }
    else -> this
}

internal fun size_t.checkError(action: String = ""): size_t = when (this) {
    ZERO -> errno.let { errno ->
        when (errno) {
            0 -> this
            else -> memScoped { throw PosixException.forErrno(action = action) }
        }
    }
    else -> this
}

typealias IOException = PosixException.IOException

@ExperimentalIoApi
sealed class PosixException(val errno: Int, message: String) : Exception(message) {
    @ExperimentalIoApi
    class BadFileDescriptorException(message: String) : PosixException(EBADF, message)

    @ExperimentalIoApi
    class TryAgainException(message: String) : PosixException(EAGAIN, message)

    @ExperimentalIoApi
    class BadMessageException(message: String) : PosixException(EBADMSG, message)

    @ExperimentalIoApi
    class InterruptedException(message: String) : PosixException(EINTR, message)

    @ExperimentalIoApi
    class InvalidArgumentException(message: String) : PosixException(EINVAL, message)

    @ExperimentalIoApi
    open class IOException(errno: Int = EIO, message: String) : PosixException(errno, message)

    @ExperimentalIoApi
    class ConnectionResetException(message: String) : IOException(ECONNRESET, message)

    @ExperimentalIoApi
    class ConnectionRefusedException(message: String) : IOException(ECONNREFUSED, message)

    @ExperimentalIoApi
    class ConnectionAbortedException(message: String) : IOException(ECONNABORTED, message)

    @ExperimentalIoApi
    class NotConnectedException(message: String) : IOException(ENOTCONN, message)

    @ExperimentalIoApi
    class TimeoutIOException(message: String) : IOException(ETIMEDOUT, message)

    @ExperimentalIoApi
    class NotSocketException(message: String) : IOException(ENOTSOCK, message)

    @ExperimentalIoApi
    class AddressAlreadyInUseException(message: String) : IOException(EADDRINUSE, message)

    @ExperimentalIoApi
    class OverflowException(message: String) : PosixException(EOVERFLOW, message)

    @ExperimentalIoApi
    class NoMemoryException(message: String) : PosixException(ENOMEM, message)

    @ExperimentalIoApi
    class PosixErrnoException(errno: Int, message: String) : PosixException(errno, "$message ($errno)")

    companion object {
        fun forErrno(errno: Int = platform.posix.errno, action: String = ""): PosixException = memScoped {
            val message = strerror(errno) + ": " + action
            when (errno) {
                EBADF -> BadFileDescriptorException(message)
                @Suppress("DUPLICATE_LABEL_IN_WHEN")
                EWOULDBLOCK, EAGAIN -> TryAgainException(message)
                EBADMSG -> BadMessageException(message)
                EINTR -> InterruptedException(message)
                EINVAL -> InvalidArgumentException(message)
                EIO -> IOException(errno, message)
                ECONNREFUSED -> ConnectionRefusedException(message)
                ECONNABORTED -> ConnectionAbortedException(message)
                ECONNRESET -> ConnectionResetException(message)
                ENOTCONN -> NotConnectedException(message)
                ETIMEDOUT -> TimeoutIOException(message)
                EOVERFLOW -> OverflowException(message)
                ENOMEM -> NoMemoryException(message)
                ENOTSOCK -> NotSocketException(message)
                EADDRINUSE -> AddressAlreadyInUseException(message)
                else -> PosixErrnoException(errno, message)
            }
        }
    }
}

private tailrec fun MemScope.strerror(errno: Int, size: size_t = 8192.convert()): String {
    val message = allocArray<ByteVar>(size.toLong())
    val result = strerror_r(errno, message, size)
    if (result == ERANGE) {
        return strerror(errno, size * 2.convert())
    }
    if (result != 0) {
        return "Unknown error ($errno)"
    }
    return message.toKString()
}
