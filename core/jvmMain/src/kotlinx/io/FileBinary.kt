package kotlinx.io

import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.UnmanagedBufferPool
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.math.min

/**
 * @param end absolute read limit relative to the start of file
 */
internal class FileInput(
    val file: Path,
    val start: Int,
    val end: Int
) : CountingInput(UnmanagedBufferPool.Instance) {

    private val channel = Files.newByteChannel(file, StandardOpenOption.READ)

    init {
        position(start)
    }

    override fun position(startIndex: Int) {
        super.position(startIndex)
        channel.position(startIndex.toLong())
    }

    override fun closeSource() {
        channel.close()
    }

    override fun fillCounting(buffer: Buffer, startIndex: Int, endIndex: Int, absoluteBufferIndex: Int): Int {
        //check end of input
        if (absoluteBufferIndex == end) return 0
        buffer.buffer.position(startIndex)
        buffer.buffer.limit(min(endIndex, end - absoluteBufferIndex - startIndex))
        return channel.read(buffer.buffer).coerceAtLeast(0)
    }

    override fun readBinary(size: Int): FileBinary {
        checkSize(size)
        return FileBinary(file, absolutePosition(), size).also {
            discardExact(size) // throws error if trying to read more, than possible
        }
    }
}

/**
 * A binary wrapped around a file or a block inside file
 */
public class FileBinary(
    val file: Path,
    val startIndex: Int = 0,
    override val size: Int = Files.size(file).toInt() - startIndex
) : Binary {
    init {
        if (!Files.exists(file)) error("File with path $file does not exist")
    }

    override fun <R> read(offset: Int, atMost: Int, block: Input.() -> R): R {
        return FileInput(
            file,
            offset + startIndex,
            min(offset + startIndex + atMost, startIndex + size)
        ).block()
    }
}

fun Path.asBinary(): FileBinary = FileBinary(this)

fun File.asBinary(): FileBinary = toPath().asBinary()