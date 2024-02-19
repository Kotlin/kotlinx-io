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
                SystemFileSystem.delete(it, false)
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

    private fun removeOnExit(path: Path) {
        files.add(path)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun readWriteFile() {
        val path = createTempPath()

        SystemFileSystem.sink(path).buffered().use {
            it.writeString("example")
        }

        SystemFileSystem.source(path).buffered().use {
            assertEquals("example", it.readLine())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun writeFlush() {
        val path = createTempPath()
        SystemFileSystem.sink(path).buffered().use {
            it.writeString("hello")
            it.flush()
            it.writeString(" world")
            it.flush()
        }

        SystemFileSystem.source(path).buffered().use {
            assertEquals("hello world", it.readLine())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun readNotExistingFile() {
        assertFailsWith<FileNotFoundException> {
            SystemFileSystem.source(createTempPath())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun readWriteMultipleSegments() {
        val path = createTempPath()

        val data = ByteArray((Segment.SIZE * 2.5).toInt()) { it.toByte() }

        SystemFileSystem.sink(path).buffered().use {
            it.write(data)
        }

        SystemFileSystem.source(path).buffered().use {
            assertArrayEquals(data, it.readByteArray())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun basicFileSystemOps() {
        val path = createTempPath()
        assertFalse(SystemFileSystem.exists(path))
        SystemFileSystem.sink(path).buffered().use {
            it.writeString("hi")
        }
        assertTrue(SystemFileSystem.exists(path))
        SystemFileSystem.delete(path)
        assertFalse(SystemFileSystem.exists(path))
    }

    @Test
    fun checkTmpDir() {
        assertTrue(SystemFileSystem.exists(SystemTemporaryDirectory))
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun atomicMove() {
        val src = createTempPath()
        val dst = createTempPath()
        SystemFileSystem.sink(src).buffered().use {
            it.writeString("hello")
        }
        SystemFileSystem.atomicMove(src, dst)
        assertFalse(SystemFileSystem.exists(src))
        assertTrue(SystemFileSystem.exists(dst))

        SystemFileSystem.source(dst).buffered().use {
            assertEquals("hello", it.readString())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun atomicMoveDir() {
        val src = createTempPath()
        val dst = createTempPath()
        SystemFileSystem.createDirectories(src)

        SystemFileSystem.atomicMove(src, dst)
        assertFalse(SystemFileSystem.exists(src))
        assertTrue(SystemFileSystem.exists(dst))
    }

    @Test
    fun deleteFile() {
        val p = createTempPath()
        assertFailsWith<FileNotFoundException> {
            SystemFileSystem.delete(p, mustExist = true)
        }
        // Should not fail
        SystemFileSystem.delete(p, false)
    }

    @Test
    fun moveNonExistingFile() {
        assertFailsWith<FileNotFoundException> {
            SystemFileSystem.atomicMove(createTempPath(), createTempPath())
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun createDirectories() {
        val p = Path(createTempPath(), "a", "b", "c")
        SystemFileSystem.createDirectories(p)
        assertTrue(SystemFileSystem.exists(p))

        assertFailsWith<IOException> {
            SystemFileSystem.createDirectories(p, true)
        }
        SystemFileSystem.createDirectories(p, false)

        val p1 = Path(p, "d")
        SystemFileSystem.createDirectories(p1)
        assertTrue(SystemFileSystem.exists(p1))

        var pr = p1
        for (i in 0..3) {
            SystemFileSystem.delete(pr)
            pr = pr.parent!!
        }

        val p2 = createTempPath()
        SystemFileSystem.sink(p2).buffered().use {
            it.writeString("hello")
        }
        assertFailsWith<IOException> { SystemFileSystem.createDirectories(p2, false) }
    }

    @Test
    fun trailingSeparatorsTrimming() {
        assertEquals(Path(".").toString(), Path(".///").toString())
        assertEquals(Path("/").toString(), Path("/").toString())
        assertEquals(Path("/..").toString(), Path("/../").toString())
        assertEquals(Path("/a/b/c").toString(), Path("/a/b/c").toString())
    }

    @Test
    fun pathParent() {
        val p = Path(SystemPathSeparator.toString(), "a", "b", "c")
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
        assertNull(Path(SystemPathSeparator.toString()).parent)

        assertEquals("..", Path("..${SystemPathSeparator}..").parent?.toString())

        assertEquals(" ", Path(SystemFileSystem.resolve(Path(".")), " ", "ws").parent?.name)
        assertEquals(" ", Path(" $SystemPathSeparator.").parent?.name)
        assertNull(Path(" ").parent)
        assertNull(Path(" /").parent)

        assertNull(Path("path////").parent)
        assertEquals(Path("."), Path("./child").parent)
        assertNull(Path("./").parent)
    }

    @Test
    fun pathConcat() {
        assertEquals(
            constructAbsolutePath("a", "b", "c"),
            Path(Path(Path(Path(SystemPathSeparator.toString()), "a"), "b"), "c").toString()
        )

        assertEquals(
            constructAbsolutePath("a", "b", "..", "c"),
            Path("${SystemPathSeparator}a", "b", "..${SystemPathSeparator}c").toString()
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
        assertEquals("", Path(SystemPathSeparator.toString()).name)
        assertEquals(".", Path(".").name)
        assertEquals("..", Path("..").name)
        assertEquals("hello.txt", Path("base", "hello.txt").name)
        assertEquals("dir", Path("dir${SystemPathSeparator}").name)
        assertEquals(" ", Path(" ").name)
        assertEquals("  ", Path(" /  ").name)
    }

    @Test
    fun isAbsolute() {
        // to make it work on both Windows and Unix, just repeat the separator twice
        val rootPath = SystemPathSeparator.repeat(2)
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
        assertNull(SystemFileSystem.metadataOrNull(path))

        SystemFileSystem.createDirectories(path)
        val dirMetadata = SystemFileSystem.metadataOrNull(path)
        assertNotNull(dirMetadata)
        assertTrue(dirMetadata.isDirectory)
        assertFalse(dirMetadata.isRegularFile)

        val filePath = Path(path, "test.txt")
        assertNull(SystemFileSystem.metadataOrNull(filePath))
        SystemFileSystem.sink(filePath).buffered().use {
            it.writeString("blablabla")
        }

        try {
            val fileMetadata = SystemFileSystem.metadataOrNull(filePath)
            assertNotNull(fileMetadata)
            assertFalse(fileMetadata.isDirectory)
            assertTrue(fileMetadata.isRegularFile)
        } finally {
            SystemFileSystem.delete(filePath, false)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun fileSize() {
        val path = createTempPath()
        val expectedSize = 123
        SystemFileSystem.sink(path).buffered().use {
            it.write(ByteArray(expectedSize))
        }
        val metadata = SystemFileSystem.metadataOrNull(path)
        assertNotNull(metadata)
        assertEquals(expectedSize.toLong(), metadata.size)

        assertEquals(-1L, SystemFileSystem.metadataOrNull(path.parent!!)!!.size)
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

        SystemFileSystem.createDirectories(childPath, true)
        assertFailsWith<IOException> { SystemFileSystem.delete(basePath) }

        SystemFileSystem.delete(childPath)
        SystemFileSystem.delete(basePath)
    }

    @Test
    fun readDirectory() {
        val dir = createTempPath()
        SystemFileSystem.createDirectories(dir)

        assertFailsWith<IOException> { SystemFileSystem.source(dir).buffered().readByte() }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun writeDirectory() {
        val dir = createTempPath()
        SystemFileSystem.createDirectories(dir)

        assertFailsWith<IOException> {
            SystemFileSystem.sink(dir).buffered().use {
                it.writeByte(0)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun appendToFile() {
        val path = createTempPath()

        SystemFileSystem.sink(path).buffered().use {
            it.writeString("first")
        }
        SystemFileSystem.sink(path).buffered().use {
            it.writeString("second")
        }
        assertEquals("second",
            SystemFileSystem.source(path).buffered().use { it.readString() })

        SystemFileSystem.sink(path, append = true).buffered().use {
            it.writeString(" third")
        }
        assertEquals("second third",
            SystemFileSystem.source(path).buffered().use { it.readString() })
    }

    @Test
    fun resolve() {
        assertFailsWith<FileNotFoundException>("Non-existing path resolution should fail") {
            SystemFileSystem.resolve(createTempPath())
        }

        val cwd = SystemFileSystem.resolve(Path("."))

        SystemFileSystem.createDirectories(Path("a"))
        removeOnExit(Path("a"))

        val childRel = Path("a", "..")
        assertEquals(cwd, SystemFileSystem.resolve(childRel))

        assertEquals(
            cwd, SystemFileSystem.resolve(cwd),
            "Absolute path resolution should not alter the path"
        )

        // root
        //   |-> a -> b
        //   |-> c -> d
        val root = createTempPath()
        SystemFileSystem.createDirectories(Path(root, "a", "b"))
        val tgt = Path(root, "c", "d")
        SystemFileSystem.createDirectories(tgt)

        val src = Path(root, "a", "..", "a", ".", "b", "..", "..", "c", ".", "d")
        try {
            // root/a/../a/./b/../../c/./d -> root/c/d
            assertEquals(SystemFileSystem.resolve(tgt), SystemFileSystem.resolve(src))
        } finally {
            // TODO: remove as soon as recursive file removal is implemented
            SystemFileSystem.delete(Path(root, "a", "b"))
            SystemFileSystem.delete(Path(root, "a"))
            SystemFileSystem.delete(Path(root, "c", "d"))
            SystemFileSystem.delete(Path(root, "c"))
        }
    }

    @Test
    fun createAnEmptyFileUsingSink() {
        val path = createTempPath()
        assertFalse(SystemFileSystem.exists(path))

        SystemFileSystem.sink(path).close()
        assertTrue(SystemFileSystem.exists(path))
        assertTrue(SystemFileSystem.metadataOrNull(path)!!.isRegularFile)
    }

    @Test
    fun closeFileSinkTwice() {
        val path = createTempPath()
        val sink = SystemFileSystem.sink(path)
        sink.close()
        sink.close() // there should be no error
    }

    @Test
    fun closeFileSourceTwice() {
        val path = createTempPath()
        SystemFileSystem.sink(path).close()
        assertTrue(SystemFileSystem.exists(path))
        val source = SystemFileSystem.source(path)
        source.close()
        source.close()  // there should be no error
    }

    private fun constructAbsolutePath(vararg parts: String): String {
        return SystemPathSeparator.toString() + parts.joinToString(SystemPathSeparator.toString())
    }

    private fun constructRelativePath(vararg parts: String): String {
        return parts.joinToString(SystemPathSeparator.toString())
    }
}
