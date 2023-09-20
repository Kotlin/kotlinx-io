/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.io

import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun segmentSizes(buffer: Buffer): List<Int> {
    var segment = buffer.head
    if (segment == null) return emptyList()

    val sizes = mutableListOf(segment.limit - segment.pos)
    segment = segment.next
    while (segment !== null) {
        sizes.add(segment.limit - segment.pos)
        segment = segment.next
    }
    return sizes
}

fun assertNoEmptySegments(buffer: Buffer) {
    assertTrue(segmentSizes(buffer).all { it != 0 }, "Expected all segments to be non-empty")
}

expect fun tempFileName(): String

private fun fromHexChar(char: Char): Int {
    val code = char.code
    return when (code) {
        in '0'.code..'9'.code -> code - '0'.code
        in 'a'.code..'f'.code -> code - 'a'.code + 10
        in 'A'.code..'F'.code -> code - 'A'.code + 10
        else -> throw NumberFormatException("Not a hexadecimal digit: $char")
    }
}

fun String.decodeHex(): ByteArray {
    if (length % 2 != 0) throw IllegalArgumentException("Even number of bytes is expected.")

    val result = ByteArray(length / 2)

    for (idx in result.indices) {
        val byte = fromHexChar(this[idx * 2]).shl(4).or(fromHexChar(this[idx * 2 + 1]))
        result[idx] = byte.toByte()
    }

    return result
}

fun Char.repeat(count: Int): String {
    return toString().repeat(count)
}

fun assertArrayEquals(a: ByteArray, b: ByteArray) {
    assertEquals(a.contentToString(), b.contentToString())
}
