/*
 * Copyright 2010-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.bytestring

import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import kotlin.random.Random

/**
 * Creates a byte string of the specified [size], filled with random bytes.
 *
 * @throws IllegalArgumentException when [size] is negative
 *
 * @sample kotlinx.io.bytestring.samples.ByteStringSamples.randomByteString
 */
public fun Random.nextByteString(size: Int): ByteString {
    require(size >= 0) { "Size must be non-negative, was $size" }
    @OptIn(UnsafeByteStringApi::class)
    return UnsafeByteStringOperations.wrapUnsafe(nextBytes(size))
}
