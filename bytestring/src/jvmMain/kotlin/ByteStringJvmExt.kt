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
public fun ByteString.toString(charset: Charset): String = getBackingArrayReference().toString(charset)

/**
 * Constructs a new byte string containing [string] encoded into bytes using [charset].
 *
 * @param string string to encode.
 * @param charset the encoding.
 */
public fun ByteString.Companion.fromString(string: String, charset: Charset): ByteString =
    wrap(string.toByteArray(charset))