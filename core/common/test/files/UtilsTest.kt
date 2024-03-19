/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {
    private fun removeTrailingSeparatorsU(path: String): String = removeTrailingSeparators(path, false)
    private fun removeTrailingSeparatorsW(path: String): String = removeTrailingSeparators(path, true)


    @Test
    fun testPathTrimmingUnix() {
        assertEquals("", removeTrailingSeparatorsU(""))
        assertEquals("/", removeTrailingSeparatorsU("/"))
        assertEquals("/", removeTrailingSeparatorsU("//"))
        assertEquals("// ", removeTrailingSeparatorsU("// "))
        assertEquals("/", removeTrailingSeparatorsU("///"))
        assertEquals("@@@", removeTrailingSeparatorsU("@@@"))
        assertEquals("/a", removeTrailingSeparatorsU("/a/"))
        assertEquals("\\", removeTrailingSeparatorsU("\\"))
        assertEquals("\\\\", removeTrailingSeparatorsU("\\\\"))
        assertEquals("\\a\\", removeTrailingSeparatorsU("\\a\\"))
        assertEquals("/\\/ ", removeTrailingSeparatorsU("/\\/ "))

        assertEquals("a//\\////\\\\//\\/\\", removeTrailingSeparatorsU("a//\\////\\\\//\\/\\"))
        assertEquals("C:\\", removeTrailingSeparatorsU("C:\\"))
        assertEquals("C:\\/\\", removeTrailingSeparatorsU("C:\\/\\"))
    }

    @Test
    fun testPathTrimmingWindows() {
        assertEquals("", removeTrailingSeparatorsW(""))
        assertEquals("/", removeTrailingSeparatorsW("/"))
        assertEquals("//", removeTrailingSeparatorsW("//"))
        assertEquals("// ", removeTrailingSeparatorsW("// "))
        assertEquals("//", removeTrailingSeparatorsW("///"))
        assertEquals("@@@", removeTrailingSeparatorsW("@@@"))
        assertEquals("/a", removeTrailingSeparatorsW("/a/"))
        assertEquals("\\", removeTrailingSeparatorsW("\\"))
        assertEquals("\\\\", removeTrailingSeparatorsW("\\\\"))
        assertEquals("\\a", removeTrailingSeparatorsW("\\a\\"))
        assertEquals("\\a", removeTrailingSeparatorsW("\\a\\\\"))
        assertEquals("/\\/ ", removeTrailingSeparatorsW("/\\/ "))

        assertEquals("a", removeTrailingSeparatorsW("a//\\////\\\\//\\/\\"))
        assertEquals("C:a", removeTrailingSeparatorsW("C:a//\\////\\\\//\\/\\"))
        assertEquals("C:\\", removeTrailingSeparatorsW("C:\\"))
        assertEquals("C:\\", removeTrailingSeparatorsW("C:\\/\\"))
    }

    @Test
    fun normalizePathWithDrive() {
        assertEquals("C:$SystemPathSeparator",
            Path("C:\\..\\..\\..\\").normalizedInternal(true, WindowsPathSeparator, UnixPathSeparator))
        assertEquals("C:..$SystemPathSeparator..$SystemPathSeparator..",
            Path("C:..\\..\\..\\").normalizedInternal(true, WindowsPathSeparator, UnixPathSeparator))
    }
}
