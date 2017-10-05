package kotlinx.io.core

fun ByteReadPacket.readUTF8Line(estimate: Int = 16, limit: Int = Int.MAX_VALUE): String? {
    if (isEmpty) return null
    val sb = StringBuilder(estimate)
    return if (readUTF8LineTo(sb, limit)) sb.toString() else null
}

fun ByteReadPacket.readBytes(n: Int = remaining): ByteArray = ByteArray(n).also { readFully(it, 0, n) }
