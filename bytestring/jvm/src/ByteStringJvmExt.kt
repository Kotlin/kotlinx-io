/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

import java.nio.charset.Charset

/**
 * Decodes the content of a byte string to a string using given [charset].
 *
 * @param charset the charset to decode data into a string.
 */
public fun ByteString.decodeToString(charset: Charset): String = getBackingArrayReference().toString(charset)

/**
 * Encodes a string into a byte string using [charset].
 *
 * @param charset the encoding.
 */
public fun String.encodeToByteString(charset: Charset): ByteString = ByteString.wrap(toByteArray(charset))
