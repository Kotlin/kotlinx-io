package kotlinx.io.errors

import kotlinx.io.core.*

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

internal fun checkPeekTo(destination: Buffer, offset: Int, min: Int, max: Int) {
    kotlinx.io.core.internal.require(offset >= 0) { "offset shouldn't be negative: $offset." }
    kotlinx.io.core.internal.require(min >= 0) { "min shouldn't be negative: $min." }
    kotlinx.io.core.internal.require(max >= min) { "max should't be less than min: max = $max, min = $min." }
    kotlinx.io.core.internal.require(min <= destination.writeRemaining) {
        "Not enough free space in the destination buffer " +
            "to write the specified minimum number of bytes: min = $min, free = ${destination.writeRemaining}."
    }
}
