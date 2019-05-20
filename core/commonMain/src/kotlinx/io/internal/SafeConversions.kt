package kotlinx.io.internal

@PublishedApi
@Suppress("NOTHING_TO_INLINE")
@Deprecated("Use function with lambda for name", replaceWith = ReplaceWith("toIntOrFail { name }"))
internal inline fun Long.toIntOrFail(name: String): Int = toIntOrFail { name }

@PublishedApi
internal inline fun Long.toIntOrFail(name: () -> String): Int {
    if (this >= Int.MAX_VALUE)
        failLongToIntConversion(this, name()) // TODO: any reason to extract this to a single-use function?
    return toInt()
}

@PublishedApi
internal fun failLongToIntConversion(value: Long, name: String): Nothing =
    throw IllegalArgumentException("Long value $value of $name doesn't fit into 32-bit integer")
