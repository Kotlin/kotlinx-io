package kotlinx.io.streams

import kotlinx.cinterop.*
import kotlinx.io.core.*
import kotlinx.io.errors.*
import kotlinx.io.internal.utils.*
import platform.posix.*

/**
 * Create a blocking [Input] reading from the specified [fileDescriptor] using [read].
 */
@ExperimentalIoApi
fun Input(fileDescriptor: Int): Input = PosixInputForFileDescriptor(fileDescriptor)

/**
 * Create a blocking [Input] reading from the specified [file] instance using [fread].
 */
@ExperimentalIoApi
fun Input(file: CPointer<FILE>): Input = PosixInputForFile(file)

private const val SZERO: ssize_t = 0
private const val ZERO: size_t = 0u

private class PosixInputForFileDescriptor(val fileDescriptor: Int) : AbstractInput() {
    private var closed = false
    init {
        check(fileDescriptor >= 0) { "Illegal fileDescriptor: $fileDescriptor" }
        check(kx_internal_is_non_blocking(fileDescriptor) == 0) { "File descriptor is in O_NONBLOCK mode." }
    }

    override fun fill(): IoBuffer? {
        val buffer = pool.borrow()
        buffer.reserveEndGap(ReservedSize)

        val size = read(fileDescriptor, buffer)
        if (size == SZERO) { // EOF
            buffer.release(pool)
            return null
        }
        if (size < 0) {
            buffer.release(pool)
            throw PosixException.forErrno(posixFunctionName = "read()").wrapIO()
        }

        return buffer
    }

    override fun closeSource() {
        if (closed) return
        closed = true

        if (close(fileDescriptor) != 0) {
            val error = errno
            if (error != EBADF) { // EBADF is already closed or not opened
                throw PosixException.forErrno(error, "close()").wrapIO()
            }
        }
    }
}

private class PosixInputForFile(val file: CPointer<FILE>) : AbstractInput() {
    private var closed = false

    override fun fill(): IoBuffer? {
        val buffer = pool.borrow()
        buffer.reserveEndGap(ReservedSize)

        val size = fread(buffer, file)
        if (size == ZERO) {
            buffer.release(pool)

            if (feof(file) != 0) return null
            throw PosixException.forErrno(posixFunctionName = "read()").wrapIO()
        }

        return buffer
    }

    override fun closeSource() {
        if (closed) return
        closed = true

        if (fclose(file) != 0) {
            throw PosixException.forErrno(posixFunctionName = "fclose()").wrapIO()
        }
    }
}
