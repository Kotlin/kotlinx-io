package kotlinx.io.gzip

import kotlinx.io.Output
import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.get
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

class GzipOutput(private val original: Output) : Output() {
    // Do not judge me
    private val baos = ByteArrayOutputStream()
    private val deflater = DeflaterOutputStream(baos)

    override fun flush(source: Buffer, length: Int) {
       for (i in 0 until length) {
           deflater.write(source[i].toInt())
       }
    }

    override fun closeSource() {
        deflater.close()
        original.writeArray(baos.toByteArray())
        //original.flush()
        original.close()
    }
}