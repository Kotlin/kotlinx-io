package kotlinx.io.core

expect class IOException(message: String, cause: Throwable?) : Exception {
    constructor(message: String)
}
