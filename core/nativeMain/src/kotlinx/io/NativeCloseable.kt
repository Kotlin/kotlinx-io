package kotlinx.io

/**
 * Closeable resource.
 */
public actual fun interface Closeable {
    public actual fun close()
}

@PublishedApi
internal actual fun Throwable.addSuppressedInternal(other: Throwable) {
}
