/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.readByteString
import kotlinx.io.write
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun testCore() {
        val buffer = Buffer()
        buffer.writeLong(0)
        assertContentEquals(ByteArray(8), buffer.readByteArray())
    }

    @Test
    fun testByteString() {
        val byteString = ByteString(0x42)
        val buffer = Buffer()
        buffer.write(byteString)

        assertEquals(ByteString(0x42), buffer.readByteString())
    }

    @Test
    fun testUseFiles() {
        try {
            SystemFileSystem.exists(Path("."))
        } catch (t: Throwable) {
            // that's fine
        }
    }
}
