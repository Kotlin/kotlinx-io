package kotlinx.io.gzip

import kotlinx.io.*
import kotlinx.io.bytes.*

@ExperimentalStdlibApi
fun main() {
    val str = "Hello, world"
    val bao = ByteArrayOutput()
    val gzipOutput = GzipOutput(bao)
    gzipOutput.writeUTF8String(str)
    gzipOutput.close()
    val array = bao.toArray()
    println("Original: " + str.encodeToByteArray().contentToString())
    println("Compressed: " +  array.contentToString())

    val gzipInput = GzipInput(ByteArrayInput(array))
    println(gzipInput.readUTF8String(str.length))
}