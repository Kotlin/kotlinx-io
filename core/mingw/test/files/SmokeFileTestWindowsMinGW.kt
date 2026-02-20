/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import platform.posix.dirname
import platform.windows.ERROR_TOO_MANY_OPEN_FILES
import platform.windows.GetConsoleCP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class)
class SmokeFileTestWindowsMinGW {
    private val testDir = Path("""./mingw/testdir""")

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun mingwProblem() {
        // Skipping test because console code page is UTF-8,
        // use when clause because I'm not sure which codepage should be skipped
        when (GetConsoleCP()) {
            65001u -> return
        }
        assertEquals("""C:\foo""", dirname("""C:\foo\bar""".cstr)!!.toKString())
        assertFails {
            assertEquals(
                """C:\あいうえお""",
                dirname("""C:\あいうえお\かきくけこ""".cstr)!!.toKString(),
            )
        }
        assertFails {
            assertEquals(
                """C:\一二三四""",
                dirname("""C:\一二三四\五六七八""".cstr)!!.toKString(),
            )
        }
    }

    @Test
    fun parent() {
        assertEquals(Path("""C:\foo"""), Path("""C:\foo\bar""").parent)
        assertEquals(Path("""C:\あいうえお"""), Path("""C:\あいうえお\かきくけこ""").parent)
        assertEquals(Path("""C:\一二三四"""), Path("""C:\一二三四\五六七八""").parent)
        assertEquals(null, Path("""C:\""").parent)
    }

    @Test
    fun uncParent() {
        assertEquals(Path("""\\server\share"""), Path("""\\server\share\dir""").parent)
        // This is a root UNC path, so parent is
        assertEquals(null, Path("""\\server\share""").parent)
    }

    @Test
    fun basename() {
        assertEquals("あいうえお", Path("""C:\あいうえお""").name)
        assertEquals("", Path("""C:\""").name)
    }

    @Test
    fun testFormatError() {
        val s = formatWin32ErrorMessage(ERROR_TOO_MANY_OPEN_FILES.toUInt())
        // it should be trimmed, drop the trailing rubbish
        assertEquals(s.trim(), s)
    }

    @Test
    fun testReadDir() {
        val expected = listOf("foo", "いろは歌", "天地玄黄")
        val actual = SystemFileSystem.list(testDir).map { it.name }.sorted()
        assertEquals(expected, actual)
    }

    @Test
    fun testExists() {
        for (path in SystemFileSystem.list(testDir)) {
            assertTrue(SystemFileSystem.exists(path), path.toString())
        }
    }
}
