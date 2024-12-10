/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io

import kotlin.test.assertEquals

fun assertByteArrayEquals(expectedUtf8: String, b: ByteArray) {
    assertEquals(expectedUtf8, b.toString(Charsets.UTF_8))
}

internal actual fun String.asUtf8ToByteArray(): ByteArray = toByteArray(Charsets.UTF_8)
