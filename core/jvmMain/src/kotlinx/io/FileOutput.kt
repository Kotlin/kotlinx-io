package kotlinx.io

import kotlinx.io.buffer.Buffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * File-based output
 */
@ExperimentalIoApi
private class FileOutput(path: Path, offset: BinarySize = 0) : Output() {

    val channel: FileChannel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE)

    init {
        channel.position(offset.toLong())
    }

    override fun flush(source: Buffer, length: Int) {
        require(length <= source.size)
        if( length<= source.size)
        source.buffer.rewind()
        val duplicate = source.buffer.duplicate().apply {
            limit(length)
        }
        channel.write(duplicate)
    }

    override fun close() {
        flush()
        channel.close()
    }
}

/**
 * Write something, using file as [Output] with given offset relative to file start.
 * Creates file if it does not exist.
 */
@ExperimentalIoApi
fun Path.write(offset: BinarySize = 0, block: Output.() -> Unit) = FileOutput(this, offset).use(block)