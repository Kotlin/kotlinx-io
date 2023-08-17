/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmokeFileTestWindows {
    @Test
    fun isAbsolute() {
        assertTrue(Path("C:\\").isAbsolute)
        assertTrue(Path("C:/").isAbsolute)
        assertTrue(Path("C:/../").isAbsolute)
        assertFalse(Path("C:file").isAbsolute)
    }
}
