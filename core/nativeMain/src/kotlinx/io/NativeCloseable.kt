package kotlinx.io

actual interface Closeable {
    actual fun close()
}

@PublishedApi
internal actual fun Throwable.addSuppressedInternal(other: Throwable) {
}

