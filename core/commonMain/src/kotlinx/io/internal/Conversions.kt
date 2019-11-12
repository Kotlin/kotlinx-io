package kotlinx.io.internal

@PublishedApi
@Suppress("NOTHING_TO_INLINE")
internal fun Long.toIntOrFail(name: String): Int = toIntOrFail { name }

@PublishedApi
internal inline fun Long.toIntOrFail(name: () -> String): Int {
    if (this >= Int.MAX_VALUE)
        failLongToIntConversion(this, name())
    return toInt()
}

/**
 * This function is needed to avoid inlining of string building code into fast path,
 * thus reducing size of inlined byte-code and improving depth of inlining
 */
@PublishedApi
internal fun failLongToIntConversion(value: Long, name: String): Nothing =
    throw IndexOutOfBoundsException("Long value $value of $name doesn't fit into 32-bit integer")
