/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package org.example

import kotlinx.io.okio.*
import kotlinx.io.readByteArray
import kotlin.test.*

public class SmokeTest {
    @Test
    fun testSinkAndSource() {
        val kxioBuffer = kotlinx.io.Buffer()
        val okioBuffer = okio.Buffer()

        kxioBuffer.write(ByteArray(10) { it.toByte() })
        okioBuffer.asKotlinxIoRawSink().write(kxioBuffer, kxioBuffer.size)
        kxioBuffer.transferFrom(okioBuffer.asKotlinxIoRawSource())

        assertContentEquals(ByteArray(10) { it.toByte() }, kxioBuffer.readByteArray())
    }

    @Test
    fun testByteString() {
        val kxio = kotlinx.io.bytestring.ByteString(1, 2, 3)
        assertEquals(3, kxio.toOkioByteString().size)

        val okio = okio.ByteString.of(4, 5, 6, 7)
        assertEquals(4, okio.toKotlinxIoByteString().size)
    }
}
