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
        // Well, it's a relative path, but Win32's PathIsRelative don't think so.
        assertTrue(Path("C:file").isAbsolute)
        assertFalse(Path("bla\\bla\\bla").isAbsolute)
        assertTrue(Path("\\\\server\\share").isAbsolute)
    }
}
