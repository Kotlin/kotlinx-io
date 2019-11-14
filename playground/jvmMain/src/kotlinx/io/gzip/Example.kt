package kotlinx.io.gzip

import kotlinx.io.*

@ExperimentalStdlibApi
fun main() {
    val str = "Hello, world"
    val bao = ByteArrayOutput()
    val gzipOutput = GzipOutput(bao)
    gzipOutput.writeUtf8String(str)
    gzipOutput.close()
    val array = bao.toByteArray()
    println("Original: " + str.encodeToByteArray().contentToString())
    println("Compressed: " +  array.contentToString())

    val gzipInput = GzipInput(ByteArrayInput(array))
    println(gzipInput.readUtf8String(str.length))
}