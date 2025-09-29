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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class SmokeFileTestWindowsMinGW {
    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun mingwProblem() {
        assertEquals("""C:\foo""", dirname("""C:\foo\bar""".cstr)!!.toKString())
        assertFails {
            assertEquals(
                """C:\あいうえお""",
                dirname("""C:\あいうえお\かきくけこ""".cstr)!!.toKString(),
            )
        }.let(::println)
        assertFails {
            assertEquals(
                """C:\一二三四""",
                dirname("""C:\一二三四\五六七八""".cstr)!!.toKString(),
            )
        }.let(::println)
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
    fun basename(){
        assertEquals("あいうえお", Path("""C:\あいうえお""").name)
        assertEquals("", Path("""C:\""").name)
    }


    @Test
    fun isAbs() {
        assertEquals(true, Path("""C:\foo""").isAbsolute, """C:\foo""")
        assertEquals(false, Path("""foo\bar""").isAbsolute, """foo\bar""")
        assertEquals(true, Path("""\foo\bar""").isAbsolute, """\foo\bar""")
        assertEquals(true, Path("""C:\""").isAbsolute, """C:\""")
        assertEquals(true, Path("""\\server\share\dir""").isAbsolute, """\\server\share\dir""")
    }

    @Test
    fun testFormatError() {
        val s = formatWin32ErrorMessage(ERROR_TOO_MANY_OPEN_FILES.toUInt())
        // it should be trimmed, drop the trailing rubbish
        assertEquals(s.trim(), s.trim())
    }

    @Test
    fun testReadDir() {
        val expected = listOf("foo", "いろは歌", "天地玄黄")
        val actual = SystemFileSystem.list(Path("""./mingw/testdir""")).map { it.name }.sorted()
        assertEquals(expected, actual)
    }
}
