/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {
    @Test
    fun testPathTrimming() {
        assertEquals("", removeTrailingSeparators("", ' '))
        assertEquals("/", removeTrailingSeparators("/", '/'))
        assertEquals("///", removeTrailingSeparators("///", '\\'))
        assertEquals("/", removeTrailingSeparators("///", '/'))
        assertEquals("/a", removeTrailingSeparators("/a/", '/'))
        assertEquals("// ", removeTrailingSeparators("// ", '/'))

        assertEquals("", removeTrailingSeparators(1, "", '/', '\\'))
        assertEquals("/", removeTrailingSeparators(1, "/", '/', '\\'))
        assertEquals("\\", removeTrailingSeparators(1, "\\", '/', '\\'))
        assertEquals("a", removeTrailingSeparators(1, "a//\\////\\\\//\\/\\", '/', '\\'))
        assertEquals("/\\/ ", removeTrailingSeparators(1,"/\\/ ", '\\', '/'))

        assertEquals("", removeTrailingSeparators(1000, "", '/', '\\'))
        assertEquals("C:\\", removeTrailingSeparators(3, "C:\\", '/', '\\'))
        assertEquals("C:\\", removeTrailingSeparators(3, "C:\\/\\", '/', '\\'))
    }
}
