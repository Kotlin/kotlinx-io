package kotlinx.io.core

import kotlinx.cinterop.*
import kotlinx.io.errors.*
import java.nio.*

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
    TODO_ERROR()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: CPointer<ByteVar>, offset: Long, length: Long) {
    TODO_ERROR()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: CPointer<ByteVar>, offset: Int, length: Int): Int {
    TODO_ERROR()
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: CPointer<ByteVar>, offset: Long, length: Long): Long {
    TODO_ERROR()
}
