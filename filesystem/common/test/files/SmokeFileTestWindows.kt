/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlin.test.*

class SmokeFileTestWindows {
    @Test
    fun isAbsolute() {
        if (!isWindows) return
        assertFalse(Path("C:").isAbsolute)
        assertTrue(Path("C:\\").isAbsolute)
        assertTrue(Path("C:/").isAbsolute)
        assertTrue(Path("C:/../").isAbsolute)
        assertFalse(Path("C:file").isAbsolute)
        assertFalse(Path("bla\\bla\\bla").isAbsolute)
        assertTrue(Path("\\\\server\\share").isAbsolute)
    }

    @Test
    fun getParent() {
        if (!isWindows) return
        assertNull(Path("C:").parent)
        assertNull(Path("C:\\").parent)
        assertNull(Path("a\\b").parent?.parent)
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
        assertEquals(".\\a", Path(".\\a\\//\\//\\\\////").toString())

        // this path could be transformed to use canonical separator on JVM
        assertEquals(Path("//").toString(), Path("//").toString())
    }
}
