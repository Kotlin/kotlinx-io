package kotlinx.io.core.internal

@PublishedApi
internal inline fun require(condition: Boolean, crossinline message: () -> String) {
    if (!condition) {
        val m = object : RequireFailureCapture() {
            override fun doFail(): Nothing {
                throw IllegalArgumentException(message())
            }
        }
        m.doFail()
    }
}

@PublishedApi
internal abstract class RequireFailureCapture {
    abstract fun doFail(): Nothing
}
