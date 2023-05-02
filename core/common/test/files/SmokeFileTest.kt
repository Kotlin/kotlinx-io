/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.*
import kotlin.test.*

class SmokeFileTest {

    @Test
    fun testBasicFile() {
        val path = Path("test.txt")
        path.sink().use {
            it.writeUtf8("example")
        }

        path.source().use {
            assertEquals("example", it.readUtf8Line())
        }
    }
}
