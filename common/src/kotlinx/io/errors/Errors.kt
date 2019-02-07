package kotlinx.io.errors

expect open class IOException(message: String, cause: Throwable?) : Exception {
    constructor(message: String)
}

expect open class EOFException(message: String) : IOException
