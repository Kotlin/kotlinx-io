/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.*
import kotlin.test.*

class SmokeFileTest {
    private val files: MutableList<Path> = arrayListOf()

    @AfterTest
    fun cleanup() {
        var lastException: Throwable? = null
        files.forEach {
            try {
                FileSystem.System.delete(it, false)
            } catch (t: Throwable) {
                lastException = t
            }
        }
        if (lastException != null) {
            throw lastException!!
        }
    }

    private fun createTempPath(): Path {
        val f = Path(tempFileName())
        files.add(f)
        return f
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testBasicFile() {
        val path = createTempPath()

        FileSystem.System.write(path).use {
            it.writeString("example")
        }

        FileSystem.System.read(path).use {
            assertEquals("example", it.readLine())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testReadWriteMultipleSegments() {
        val path = createTempPath()

        val data = ByteArray((Segment.SIZE * 2.5).toInt()) { it.toByte() }

        FileSystem.System.write(path).use {
            it.write(data)
        }

        FileSystem.System.read(path).use {
            assertArrayEquals(data, it.readByteArray())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testFsOps() {
        val path = createTempPath()
        assertFalse(FileSystem.System.exists(path))
        FileSystem.System.write(path).use {
            it.writeString("hi")
        }
        assertTrue(FileSystem.System.exists(path))
        FileSystem.System.delete(path)
        assertFalse(FileSystem.System.exists(path))
    }

    @Test
    fun checkTmpDir() {
        assertTrue(FileSystem.System.exists(FileSystem.System.temporaryDirectory))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testMove() {
        val src = createTempPath()
        val dst = createTempPath()
        FileSystem.System.write(src).use {
            it.writeString("hello")
        }
        FileSystem.System.atomicMove(src, dst)
        assertFalse(FileSystem.System.exists(src))
        assertTrue(FileSystem.System.exists(dst))

        FileSystem.System.read(dst).use {
            assertEquals("hello", it.readString())
        }
    }

    @Test
    fun deleteFile() {
        val p = createTempPath()
        assertFailsWith<IOException> {
            FileSystem.System.delete(p, mustExist = true)
        }
        // Should not fail
        FileSystem.System.delete(p, false)
    }

    @Test
    fun moveNonExistingFile() {
        assertFailsWith<IOException> {
            FileSystem.System.atomicMove(createTempPath(), createTempPath())
        }
    }

    // TODO: createDirectories

    @Test
    fun pathParent() {
        val p = Path("/a/b/c/")
        assertEquals("/a/b", p.parent()?.asString())
        assertEquals("/a", p.parent()?.parent()?.asString())
        assertEquals("/", p.parent()?.parent()?.parent()?.asString())
        assertNull(p.parent()?.parent()?.parent()?.parent())

        val p1 = Path("home/../lib")
        assertEquals("home/..", p1.parent()?.asString())
        assertEquals("home", p1.parent()?.parent()?.asString())
        assertNull(p1.parent()?.parent()?.parent())
    }
}
