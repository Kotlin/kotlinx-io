package kotlinx.io.errors

expect open class IOException(message: String, cause: Throwable?) : Exception {
    constructor(message: String)
}
