package kotlinx.io.streams

import kotlinx.cinterop.*
import kotlinx.io.core.*
import kotlinx.io.internal.utils.*
import platform.posix.*

private const val ZERO: size_t = 0u

/**
 * Create a blocking [Output] writing to the specified [fileDescriptor] using [write].
 */
@ExperimentalIoApi
fun Output(fileDescriptor: Int): Output = PosixFileDescriptorOutput(fileDescriptor)

/**
 * Create a blocking [Output] writing to the specified [file] instance using [fwrite].
 */
@ExperimentalIoApi
fun Output(file: CPointer<FILE>): Output = PosixFileInstanceOutput(file)

private class PosixFileDescriptorOutput(val fileDescriptor: Int) : AbstractOutput() {
    init {
        check(fileDescriptor >= 0) { "Illegal fileDescriptor: $fileDescriptor" }
        check(kx_internal_is_non_blocking(fileDescriptor) == 0) { "File descriptor is in O_NONBLOCK mode." }
    }

    override fun flush(buffer: IoBuffer) {
        while (buffer.canRead()) {
            if (write(fileDescriptor, buffer) <= 0) {
                throw PosixException.forErrno()
            }
        }
    }

    override fun closeDestination() {
        close(fileDescriptor)
    }
}

private class PosixFileInstanceOutput(val file: CPointer<FILE>) : AbstractOutput() {
    override fun flush(buffer: IoBuffer) {
        while (buffer.canRead()) {
            if (fwrite(buffer, file) == ZERO) {
                throw PosixException.forErrno()
            }
        }
    }

    override fun closeDestination() {
        fclose(file)
    }
}
