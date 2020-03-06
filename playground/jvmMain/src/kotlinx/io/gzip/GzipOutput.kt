package kotlinx.io.gzip

import kotlinx.io.*
import kotlinx.io.buffer.*
import java.io.*
import java.util.zip.*

class GzipOutput(private val original: Output) : Output() {
    // Do not judge me
    private val baos = ByteArrayOutputStream()
    private val deflater = DeflaterOutputStream(baos)

    override fun flush(source: Buffer, startIndex: Int, endIndex: Int): Boolean {
        for (i in startIndex until endIndex) {
            deflater.write(source[i].toInt())
        }

        return true
    }

    override fun closeSource() {
        deflater.close()
        original.writeByteArray(baos.toByteArray())
        original.flush()
        original.close()
    }
}