package kotlinx.io.bits

// TODO: length default argument should be this.size - offset but it doesn't work due to KT-29920
/**
 * Execute [block] of code providing a temporary instance of [Memory] view of this byte array range
 * starting at the specified [offset] and having the specified bytes [length].
 * By default, if neither [offset] nor [length] specified, the whole array is used.
 * An instance of [Memory] provided into the [block] should be never captured and used outside of lambda.
 */
expect fun <R> ByteArray.useMemory(offset: Int = 0, length: Int, block: (Memory) -> R): R
