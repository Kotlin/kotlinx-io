package kotlinx.io.core

import kotlinx.io.errors.*
import java.nio.*

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: ByteBuffer, length: Int = dst.remaining()) {
    TODO_ERROR()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: ByteBuffer, length: Int = dst.remaining()): Int {
    TODO_ERROR()
}
