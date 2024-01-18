/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.isWindows
import kotlin.test.*

class SmokeFileTestWindows {
    @Test
    fun isAbsolute() {
        if (!isWindows) return
        assertTrue(Path("C:\\").isAbsolute)
        assertTrue(Path("C:/").isAbsolute)
        assertTrue(Path("C:/../").isAbsolute)
        // Well, it's a relative path, but Win32's PathIsRelative don't think so.
        assertTrue(Path("C:file").isAbsolute)
        assertFalse(Path("bla\\bla\\bla").isAbsolute)
        assertTrue(Path("\\\\server\\share").isAbsolute)
    }

    @Test
    fun getParent() {
        if (!isWindows) return
        assertNull(Path("C:\\").parent)
        assertNull(Path("a\\b").parent?.parent)
        assertEquals(Path("\\\\server"), Path("\\\\server\\share").parent)
        assertEquals(Path("C:\\"), Path("C:\\Program Files").parent)
        assertEquals(Path("C:\\Program Files"), Path("C:\\Program Files/Java").parent)
    }

    @Test
    fun trailingSeparatorsTrimming() {
        if (!isWindows) return
        assertEquals(".", Path(".\\").toString())
        assertEquals("C:\\", Path("C:\\").toString())
        assertEquals("C:\\", Path("C:\\\\").toString())
        assertEquals("\\\\", Path("\\\\").toString())
        assertEquals("//", Path("//").toString())
        assertEquals(".\\a", Path(".\\a\\//\\//\\\\////").toString())
    }
}
