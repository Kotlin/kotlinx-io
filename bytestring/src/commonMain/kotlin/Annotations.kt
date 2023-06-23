/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

/**
 * Marks declarations whose usage may brake some ByteString invariants.
 *
 * Consider using other APIs instead when possible.
 * Otherwise, make sure to read documentation describing an unsafe API.
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is a unsafe API and its use may corrupt the data stored in a byte string. " +
            "Make sure you fully read and understand documentation of the declaration that is marked as an unsafe API."
)
public annotation class UnsafeByteStringApi