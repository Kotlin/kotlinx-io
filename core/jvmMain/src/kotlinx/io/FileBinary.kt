package kotlinx.io

import kotlinx.io.buffer.Buffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@ExperimentalIoApi
private class FileInput(
    val path: Path,
    val offset: Int = 0,
    size: Int = Files.size(path).toInt() - offset
) : Input() {

    val fileChannel = FileChannel.open(path, StandardOpenOption.READ).also {
        it.position(offset.toLong())
    }

    var toRead = size

    init {
        if( offset + size > Files.size(path)) throw EOFException("Trying to read beyond end of file")
    }

    override fun closeSource() {
        fileChannel.close()
    }

    override fun fill(buffer: Buffer): Int {
        buffer.buffer.rewind()
        val read: Int = if (toRead >= buffer.size) {
            fileChannel.read(buffer.buffer)
        } else {
            val limitedBuffer = buffer.buffer.duplicate().apply {
                limit(toRead)
            }
            fileChannel.read(limitedBuffer)
        }

        toRead -= read

        return read
    }
}

/**
 * A lazily opened read-only file binary block with path [path], offset [offset] from the start of the file and length [size]
 */
@ExperimentalIoApi
class FileBinary(val path: Path, private val offset: Int = 0, size: Int? = null) : RandomAccessBinary {

    override val size: Int = size ?: (Files.size(path) - offset).toInt()

    init {
        if (size != null && Files.size(path) < offset.toLong() + size.toLong()) {
            error("Can't read binary from file. File is to short.")
        }
    }

    override fun <R> read(from: Int, atMost: Int, block: Input.() -> R): R {
        val input = FileInput(path, offset + from, kotlin.math.min(atMost, this@FileBinary.size))
        return input.use(block)
    }
}

/**
 * Create a lazy binary from given [Path]. The file is not opened until [read] operation is invoked.
 * Multiple reads could be done simultaneously.
 */
@ExperimentalIoApi
fun Path.asBinary(offset: Int = 0, size: Int? = null): FileBinary = FileBinary(this, offset, size)

/**
 * Read from file once and close it after read
 */
@ExperimentalIoApi
fun <R> Path.read(offset: Int = 0, block: Input.() -> R): R {
    return FileInput(this, offset).use(block)
}