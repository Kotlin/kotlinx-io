package kotlinx.io.core

/**
 * Read a string line considering optionally specified [estimate] but up to optional [limit] characters length
 * (does fail once limit exceeded) or return `null` if the packet is empty
 */
fun ByteReadPacket.readUTF8Line(estimate: Int = 16, limit: Int = Int.MAX_VALUE): String? {
    if (isEmpty) return null
    val sb = StringBuilder(estimate)
    return if (readUTF8LineTo(sb, limit)) sb.toString() else null
}

/**
 * Read exactly [n] bytes (consumes all remaining if [n] is not specified). Does fail if not enough bytes remaining
 */
fun ByteReadPacket.readBytes(n: Int = remaining): ByteArray = ByteArray(n).also { readFully(it, 0, n) }
