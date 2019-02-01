package kotlinx.io.core

actual class IOException actual constructor(message: String, cause: Throwable?) : Exception(message, cause) {
    actual constructor(message: String) : this(message, null)
}
