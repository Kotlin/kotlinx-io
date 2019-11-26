package kotlinx.io.gzip

import kotlinx.io.ByteArrayInput
import kotlinx.io.bytes.ByteArrayOutput
import kotlinx.io.readUTF8String
import kotlinx.io.writeUTF8String

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