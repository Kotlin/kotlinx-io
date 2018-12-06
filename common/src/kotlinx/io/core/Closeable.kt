package kotlinx.io.core

expect interface Closeable {
    fun close()
}

inline fun <C : Closeable, R> C.use(block: (C) -> R): R {
    try {
        val result = block(this)
        close()
        return result
    } catch (first: Throwable) {
        try {
            close()
        } catch (second: Throwable) {
            first.addSuppressedInternal(second)
        }
        throw first
    }
}

@PublishedApi
internal expect fun Throwable.addSuppressedInternal(other: Throwable)
