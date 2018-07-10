package kotlinx.io.core

expect interface Closeable {
    fun close()
}

inline fun <R> Closeable.use(block: () -> R): R {
    try {
        val result = block()
        close()
        return result
    } catch (first: Throwable) {
        try {
            close()
        } catch (second: Throwable) {
            first.addSuppressedInt(second)
        }
        throw first
    }
}

@PublishedApi
internal expect fun Throwable.addSuppressedInt(other: Throwable)
