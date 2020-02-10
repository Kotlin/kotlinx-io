package kotlinx.io

import kotlin.test.*

fun assertArrayEquals(expected: ByteArray, actual: ByteArray) {
    assertEquals(expected.size, actual.size)
    assertEquals(expected.toHexString(), actual.toHexString())
}

fun ByteArray.toHexString(): String = "0x" + joinToString("") {
    it.toUInt().toString(16).padStart(2, '0')
}

internal fun Bytes.useInput(block: Input.() -> Unit) {
    try {
        block(input())
    } finally {
        close()
    }
}