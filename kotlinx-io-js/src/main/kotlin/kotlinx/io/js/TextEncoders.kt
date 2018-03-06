package kotlinx.io.js

import org.khronos.webgl.*

internal external class TextEncoder() {
    val encoding: String

    fun encode(input: String): Uint8Array
}
