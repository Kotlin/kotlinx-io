/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.*
import kotlin.test.*

class SmokeFileTest {
    private var tempFile: String? = null

    @BeforeTest
    fun setup() {
        tempFile = createTempFile()
    }

    @AfterTest
    fun cleanup() {
        deleteFile(tempFile!!)
    }

    @Test
    fun testBasicFile() {
        val path = Path(tempFile!!)
        path.sink().use {
            it.writeUtf8("example")
        }

        path.source().use {
            assertEquals("example", it.readUtf8Line())
        }
    }
}
