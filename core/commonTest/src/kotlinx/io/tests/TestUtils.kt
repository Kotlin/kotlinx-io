package kotlinx.io.tests

import kotlinx.io.*

internal fun Bytes.useInput(block: Input.() -> Unit) {
    use {
        block(it.input())
    }
}