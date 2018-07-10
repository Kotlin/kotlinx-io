package kotlinx.io.core

actual interface Closeable {
    actual fun close()
}

@PublishedApi
internal actual fun Throwable.addSuppressedInt(other: Throwable) {
}
