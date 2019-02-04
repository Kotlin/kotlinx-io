package kotlinx.io.errors

expect class IOException(message: String, cause: Throwable?) : Exception {
    constructor(message: String)
}
