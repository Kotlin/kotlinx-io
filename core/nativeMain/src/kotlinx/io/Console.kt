package kotlinx.io

import kotlinx.cinterop.*
import kotlinx.io.buffer.*
import platform.posix.*

@SharedImmutable
private val MAX_POSIX_READ = SSIZE_MAX.coerceAtMost(Int.MAX_VALUE.convert()).toInt()

public actual object Console {
    public actual val input: Input = SystemIn
    public actual val output: Output = SystemOut
    public actual val error: Output = SystemErr
}

// TODO these two objects will be rewritten on top of FileDescriptor[Input|Output] with proper exception handling etc.
// Now it's just a stub that works in the most basic use-cases
@ThreadLocal // TODO decide something on thread locality
public object SystemIn : Input() {

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        // Read no more than Int.MAX_VALUE and SSIZE_MAX
        val bytesToRead = buffer.size.coerceAtMost(MAX_POSIX_READ)
        val read = buffer.usePointer {
            read(STDIN_FILENO, it, bytesToRead.convert()).toInt()
        }
        if (read == -1) {
            throw IOException("Posix write error: $errno")
        }
        return read
    }

    override fun closeSource() {
        throw IllegalStateException("System.in cannot be closed")
    }
}

@ThreadLocal
public object SystemOut : Output() {
    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
        val error = source.usePointer {
            val size = endIndex - startIndex
            write(STDOUT_FILENO, it + startIndex, size.convert()).toInt()
        }
        if (error == -1) {
            throw IOException("Posix write error: $errno")
        }
    }

    override fun closeSource() {
        throw IllegalStateException("System.out cannot be closed")
    }
}

@ThreadLocal
public object SystemErr : Output() {

    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
        val error = source.usePointer {
            val size = startIndex - endIndex
            write(STDERR_FILENO, it + startIndex, size.convert()).toInt()
        }
        if (error == -1) {
            throw IOException("Posix write error: $errno")
        }
    }

    override fun closeSource() {
        throw IllegalStateException("System.err cannot be closed")
    }
}
