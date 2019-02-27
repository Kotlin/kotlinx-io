package kotlinx.io.errors

expect open class IOException(message: String, cause: Throwable?) : Exception {
    constructor(message: String)
}

expect open class EOFException(message: String) : IOException

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Not implemented.", level = DeprecationLevel.ERROR)
fun <R> TODO_ERROR(value: R): Nothing = TODO("Not implemented. Value is $value")

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Not implemented.", level = DeprecationLevel.ERROR)
fun TODO_ERROR(): Nothing = TODO("Not implemented.")
