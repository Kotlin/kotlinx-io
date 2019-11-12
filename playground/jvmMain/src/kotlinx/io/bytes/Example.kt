package kotlinx.io.bytes

import kotlinx.io.*

@ExperimentalStdlibApi
fun main() {
    val strings = listOf("Hello, ", "World\n")
    val bao = ByteArrayOutput()
    strings.forEach { bao.writeUTF8String(it) }
    val array = bao.toArray()
    println("Original: " + strings.joinToString { it.encodeToByteArray().contentToString() })
    println("Written: " +  array.contentToString())

    val bai = ByteArrayInput(array)
    println(bai.readUTF8Line())
}