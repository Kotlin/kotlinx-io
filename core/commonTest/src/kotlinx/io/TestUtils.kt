package kotlinx.io

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.test.assertEquals

fun assertArrayEquals(expected: ByteArray, actual: ByteArray) {
    assertEquals(expected.size, actual.size, "Expected array lengths to be equal")
    assertEquals(expected.toHexString(), actual.toHexString())
}

fun ByteArray.toHexString(): String = "0x" + joinToString("") { it.toUInt().toString(16).padStart(2, '0') }

internal inline fun Bytes.useInput(block: Input.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    try {
        block(input())
    } finally {
        close()
    }
}

fun StringInput(string: String) = ByteArrayInput(string.encodeToByteArray())
