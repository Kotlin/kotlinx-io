/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring.unsafe

import kotlinx.io.bytestring.ByteString

/**
 * Collection of helper functions providing unsafe access to the [ByteString]'s underlying byte sequence or allowing
 * to wrap byte arrays into [ByteString] without copying the array.
 *
 * These functions are provided for performance sensitive cases where it is known that the data accessed
 * in an unsafe manner won't be modified. Modification of the data backing byte strings may lead to unpredicted
 * consequences in the code using the byte string and should be avoided at all costs.
 */
@UnsafeByteStringApi
public object UnsafeByteStringOperations {
    /**
     * Creates a new byte string by wrapping [array] without copying it.
     * Make sure that the wrapped array won't be modified during the lifespan of the returned byte string.
     *
     * @param array the array to wrap into the byte string.
     */
    public fun wrapUnsafe(array: ByteArray): ByteString = ByteString.wrap(array)

    /**
     * Applies [block] to a reference to the underlying array.
     *
     * This method invokes [block] on a reference to the underlying array, not to its copy.
     * Consider using [ByteString.toByteArray] if it's impossible to guarantee that the array won't be modified.
     */
    public inline fun withByteArrayUnsafe(byteString: ByteString, block: (ByteArray) -> Unit) {
        block(byteString.getBackingArrayReference())
    }
}
