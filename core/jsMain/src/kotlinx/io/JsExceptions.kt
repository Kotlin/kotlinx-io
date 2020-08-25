package kotlinx.io

public actual open class IOException actual constructor(message: String, cause: Throwable?) : Exception(message, cause) {
    public actual constructor(message: String) : this(message, null)
}

public actual open class EOFException actual constructor(message: String) : IOException(message)
