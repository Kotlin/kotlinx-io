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
    fun readWriteFile() {
        val path = createTempPath()

        FileSystem.System.sink(path).buffered().use {
            it.writeString("example")
        }

        FileSystem.System.source(path).buffered().use {
            assertEquals("example", it.readLine())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun readNotExistingFile() {
        assertFailsWith<FileNotFoundException> {
            FileSystem.System.source(createTempPath()).buffered().use {
                it.readByte()
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun readWriteMultipleSegments() {
        val path = createTempPath()

        val data = ByteArray((Segment.SIZE * 2.5).toInt()) { it.toByte() }

        FileSystem.System.sink(path).buffered().use {
            it.write(data)
        }

        FileSystem.System.source(path).buffered().use {
            assertArrayEquals(data, it.readByteArray())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun basicFileSystemOps() {
        val path = createTempPath()
        assertFalse(FileSystem.System.exists(path))
        FileSystem.System.sink(path).buffered().use {
            it.writeString("hi")
        }
        assertTrue(FileSystem.System.exists(path))
        FileSystem.System.delete(path)
        assertFalse(FileSystem.System.exists(path))
    }

    @Test
    fun checkTmpDir() {
        assertTrue(FileSystem.System.exists(FileSystem.SystemTemporaryDirectory))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun atomicMove() {
        val src = createTempPath()
        val dst = createTempPath()
        FileSystem.System.sink(src).buffered().use {
            it.writeString("hello")
        }
        FileSystem.System.atomicMove(src, dst)
        assertFalse(FileSystem.System.exists(src))
        assertTrue(FileSystem.System.exists(dst))

        FileSystem.System.source(dst).buffered().use {
            assertEquals("hello", it.readString())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun atomicMoveDir() {
        val src = createTempPath()
        val dst = createTempPath()
        FileSystem.System.createDirectories(src)

        FileSystem.System.atomicMove(src, dst)
        assertFalse(FileSystem.System.exists(src))
        assertTrue(FileSystem.System.exists(dst))
    }

    @Test
    fun deleteFile() {
        val p = createTempPath()
        assertFailsWith<FileNotFoundException> {
            FileSystem.System.delete(p, mustExist = true)
        }
        // Should not fail
        FileSystem.System.delete(p, false)
    }

    @Test
    fun moveNonExistingFile() {
        assertFailsWith<FileNotFoundException> {
            FileSystem.System.atomicMove(createTempPath(), createTempPath())
        }
    }

    @Test
    fun createDirectories() {
        val p = Path(createTempPath(), "a", "b", "c")
        FileSystem.System.createDirectories(p)
        assertTrue(FileSystem.System.exists(p))

        assertFailsWith<IOException> {
            FileSystem.System.createDirectories(p, true)
        }
        FileSystem.System.createDirectories(p, false)

        val p1 = Path(p, "d")
        FileSystem.System.createDirectories(p1)
        assertTrue(FileSystem.System.exists(p1))

        var pr = p1
        for (i in 0..3) {
            FileSystem.System.delete(pr)
            pr = pr.parent!!
        }
    }

    @Test
    fun pathParent() {
        val p = Path(Path.separator.toString(), "a", "b", "c")
        assertEquals(constructAbsolutePath("a", "b"), p.parent?.toString())
        assertEquals(constructAbsolutePath("a"), p.parent?.parent?.toString())
        assertEquals(constructAbsolutePath(), p.parent?.parent?.parent?.toString())
        assertNull(p.parent?.parent?.parent?.parent)

        val p1 = Path("home", "..", "lib")
        assertEquals(constructRelativePath("home", ".."), p1.parent?.toString())
        assertEquals("home", p1.parent?.parent?.toString())
        assertNull(p1.parent?.parent?.parent)

        assertNull(Path("").parent)
        assertNull(Path(".").parent)
        assertNull(Path("..").parent)
        assertNull(Path(Path.separator.toString()).parent)

        assertEquals("..", Path("..${Path.separator}..").parent?.toString())
    }

    @Test
    fun pathConcat() {
        assertEquals(
            constructAbsolutePath("a", "b", "c"),
            Path(Path(Path(Path(Path.separator.toString()), "a"), "b"), "c").toString()
        )

        assertEquals(
            constructAbsolutePath("a", "b", "..", "c"),
            Path("${Path.separator}a", "b", "..${Path.separator}c").toString()
        )

        assertEquals(
            constructRelativePath("a", "b", "c"),
            Path("", "a", "b", "c").toString()
        )
    }

    @Test
    fun fileName() {
        assertEquals("", Path("").name)
        assertEquals("hello", Path("hello").name)
        assertEquals("", Path(Path.separator.toString()).name)
        assertEquals(".", Path(".").name)
        assertEquals("..", Path("..").name)
        assertEquals("hello.txt", Path("base", "hello.txt").name)
        assertEquals("dir", Path("dir${Path.separator}").name)
    }

    @Test
    fun isAbsolute() {
        // to make it work on both Windows and Unix, just repeat the separator twice
        val rootPath = Path.separator.repeat(2)
        assertTrue(Path(rootPath).isAbsolute)
        assertFalse(Path("").isAbsolute)
        assertFalse(Path("..").isAbsolute)
        assertFalse(Path(".").isAbsolute)
        assertTrue(Path(rootPath, "a", "b", "c").isAbsolute)
        assertFalse(Path("hello", "filesystem").isAbsolute)
        assertTrue(Path(rootPath, "lib", "..", "usr", "lib").isAbsolute)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun fileMetadata() {
        val path = createTempPath()
        assertNull(FileSystem.System.metadataOrNull(path))

        FileSystem.System.createDirectories(path)
        val dirMetadata = FileSystem.System.metadataOrNull(path)
        assertNotNull(dirMetadata)
        assertTrue(dirMetadata.isDirectory)
        assertFalse(dirMetadata.isRegularFile)

        val filePath = Path(path, "test.txt")
        assertNull(FileSystem.System.metadataOrNull(filePath))
        FileSystem.System.sink(filePath).buffered().use {
            it.writeString("blablabla")
        }

        try {
            val fileMetadata = FileSystem.System.metadataOrNull(filePath)
            assertNotNull(fileMetadata)
            assertFalse(fileMetadata.isDirectory)
            assertTrue(fileMetadata.isRegularFile)
        } finally {
            FileSystem.System.delete(filePath, false)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun fileSize() {
        val path = createTempPath()
        val expectedSize = 123
        FileSystem.System.sink(path).buffered().use {
            it.write(ByteArray(expectedSize))
        }
        val metadata = FileSystem.System.metadataOrNull(path)
        assertNotNull(metadata)
        assertEquals(expectedSize.toLong(), metadata.size)

        assertEquals(-1L, FileSystem.System.metadataOrNull(path.parent!!)!!.size)
    }

    @Test
    fun pathEquality() {
        val p0 = Path("/", "a", "b", "c")
        assertEquals(p0, p0)

        assertEquals(p0, Path("/", "a", "b", "c"))

        // Paths compared by the string representation, so even if two Paths represent
        // the same entity in the file system, they may not be equal
        // (but it depends on how a platform treats paths).
        assertNotEquals(p0, Path(p0, "d", ".."))
    }

    @Test
    fun deleteNonEmptyDirectory() {
        val basePath = createTempPath()
        val childPath = Path(basePath, "child")

        FileSystem.System.createDirectories(childPath, true)
        assertFailsWith<IOException> { FileSystem.System.delete(basePath) }

        FileSystem.System.delete(childPath)
        FileSystem.System.delete(basePath)
    }

    @Test
    fun readDirectory() {
        val dir = createTempPath()
        FileSystem.System.createDirectories(dir)

        assertFailsWith<IOException> { FileSystem.System.source(dir).buffered().readByte() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun writeDirectory() {
        val dir = createTempPath()
        FileSystem.System.createDirectories(dir)

        assertFailsWith<IOException> {
            FileSystem.System.sink(dir).buffered().use {
                it.writeByte(0)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun appendToFile() {
        val path = createTempPath()

        FileSystem.System.sink(path).buffered().use {
            it.writeString("first")
        }
        FileSystem.System.sink(path).buffered().use {
            it.writeString("second")
        }
        assertEquals("second",
            FileSystem.System.source(path).buffered().use { it.readString() })

        FileSystem.System.sink(path, append = true).buffered().use {
            it.writeString(" third")
        }
        assertEquals("second third",
            FileSystem.System.source(path).buffered().use { it.readString() })

    }

    private fun constructAbsolutePath(vararg parts: String): String {
        return Path.separator.toString() + parts.joinToString(Path.separator.toString())
    }

    private fun constructRelativePath(vararg parts: String): String {
        return parts.joinToString(Path.separator.toString())
    }
}
