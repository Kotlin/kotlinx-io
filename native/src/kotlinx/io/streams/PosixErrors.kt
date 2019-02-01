package kotlinx.io.streams

import kotlinx.cinterop.*
import kotlinx.io.core.*
import kotlinx.io.internal.utils.*
import platform.posix.*

private const val ZERO: size_t = 0u
private val s: KX_SOCKET = 0.convert() // do not remove! This is required to hold star import for strerror_r

internal fun Int.checkError(action: String = ""): Int = when {
    this < 0 -> memScoped { throw PosixException.forErrno(posixFunctionName = action) }
    else -> this
}

internal fun Long.checkError(action: String = ""): Long = when {
    this < 0 -> memScoped { throw PosixException.forErrno(posixFunctionName = action) }
    else -> this
}

internal fun size_t.checkError(action: String = ""): size_t = when (this) {
    ZERO -> errno.let { errno ->
        when (errno) {
            0 -> this
            else -> memScoped { throw PosixException.forErrno(posixFunctionName = action) }
        }
    }
    else -> this
}

private val KnownPosixErrors = mapOf<Int, String>(
    EBADF to "EBADF",
    EWOULDBLOCK to "EWOULDBLOCK",
    EAGAIN to "EAGAIN",
    EBADMSG to "EBADMSG",
    EINTR to "EINTR",
    EINVAL to "EINVAL",
    EIO to "EIO",
    ECONNREFUSED to "ECONNREFUSED",
    ECONNABORTED to "ECONNABORTED",
    ECONNRESET to "ECONNRESET",
    ENOTCONN to "ENOTCONN",
    ETIMEDOUT to "ETIMEDOUT",
    EOVERFLOW to "EOVERFLOW",
    ENOMEM to "ENOMEM",
    ENOTSOCK to "ENOTSOCK",
    EADDRINUSE to "EADDRINUSE",
    ENOENT to "ENOENT"
)

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
    class NoSuchFileException(message: String) : IOException(ENOENT, message)

    @ExperimentalIoApi
    class OverflowException(message: String) : PosixException(EOVERFLOW, message)

    @ExperimentalIoApi
    class NoMemoryException(message: String) : PosixException(ENOMEM, message)

    @ExperimentalIoApi
    class PosixErrnoException(errno: Int, message: String) : PosixException(errno, "$message ($errno)")

    companion object {
        @ExperimentalIoApi
        fun forErrno(errno: Int = platform.posix.errno, posixFunctionName: String? = null): PosixException = memScoped {
            val posixConstantName = KnownPosixErrors[errno]
            val posixErrorCodeMessage = when {
                posixConstantName == null -> "POSIX error $errno"
                else -> "$posixConstantName ($errno)"
            }

            val message = when {
                posixFunctionName.isNullOrBlank() -> posixErrorCodeMessage + ": " + strerror(errno)
                else -> "$posixFunctionName failed, $posixErrorCodeMessage: ${strerror(errno)}"
            }

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
                ENOENT -> NoSuchFileException(message)
                else -> PosixErrnoException(errno, message)
            }
        }
    }
}

internal fun PosixException.wrapIO(): IOException =
    IOException("I/O operation failed due to posix error code $errno", this)

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
