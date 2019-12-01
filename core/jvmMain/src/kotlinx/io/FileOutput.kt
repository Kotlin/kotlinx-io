package kotlinx.io

import kotlinx.io.buffer.Buffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * File-based output
 */
@ExperimentalIoApi
private class FileOutput(path: Path, offset: Int = 0) : Output() {

    val channel: FileChannel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE)

    init {
        channel.position(offset.toLong())
    }

    override fun flush(source: Buffer, length: Int) {
        require(length <= source.size)
        if (length > source.size) error("Can't read $length bytes from buffer with size ${source.size}")

        val duplicate = source.buffer.duplicate().apply {
            //rewind()
            position(0)
            limit(length)
        }

        channel.write(duplicate)
    }

    override fun closeSource() {
        channel.close()
    }
}

/**
 * Write something, using file as [Output] with given offset relative to file start.
 * Creates file if it does not exist.
 */
@ExperimentalIoApi
fun Path.write(offset: Int = 0, block: Output.() -> Unit) = FileOutput(this, offset).use(block)