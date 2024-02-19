/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.readLine
import kotlinx.io.writeString
import kotlin.test.*

class WasiFsTest {
    @Test
    fun hasTemp() {
        val preopen = PreOpens.preopens.first()
        assertEquals(3, preopen.fd)
        assertEquals(Path("/tmp"), preopen.path)
    }

    @Test
    fun multiplePreOpens() {
        fun checkPreOpen(forPath: String, expected: String?) {
            val preOpen = PreOpens.findPreopenOrNull(Path(forPath))
            if (expected == null) {
                assertNull(preOpen)
            } else {
                assertNotNull(preOpen)
                assertEquals(Path(expected), preOpen.path)
            }
        }

        checkPreOpen(forPath = "/data", expected = null)
        checkPreOpen(forPath = "/tmp", expected = "/tmp")
        checkPreOpen(forPath = "/tmp/a", expected = "/tmp")
        checkPreOpen(forPath = "/var", expected = null)
        checkPreOpen(forPath = "/var/what", expected = null)
        checkPreOpen(forPath = "/var/log", expected = "/var/log")
        checkPreOpen(forPath = "/tmp ", expected = null)
        checkPreOpen(forPath = "/tmpry", expected = null)
        checkPreOpen(forPath = "/var/logging", expected = null)
    }

    @Test
    fun resolveRelativePath() {
        val path = Path("a", "b", "c")
        SystemFileSystem.createDirectories(path)
        try {
            val resolved = SystemFileSystem.resolve(path)
            assertEquals(Path(PreOpens.roots.first(), "a", "b", "c"), resolved)
        } finally {
            SystemFileSystem.delete(path)
            SystemFileSystem.delete(path.parent!!)
            SystemFileSystem.delete(path.parent!!.parent!!)
        }
    }

    @Test
    fun createDirectoryForRelativePath() {
        val path = Path("rel")
        SystemFileSystem.createDirectories(path)
        try {
            val metadata = SystemFileSystem.metadataOrNull(Path(PreOpens.roots.first(), "rel"))
            assertNotNull(metadata)
            assertTrue(metadata.isDirectory)
        } finally {
            SystemFileSystem.delete(path, false)
        }
    }

    @Test
    fun resolution() {
        val resolved = SystemFileSystem.resolve(Path("/tmp/../../a/../b/../../tmp"))
        assertEquals(Path("/tmp"), resolved)

        SystemFileSystem.createDirectories(Path("/tmp/a/b/c/d/e"))
        try {
            WasiFileSystem.symlink(Path("/tmp/a/b/c/d/e"), Path("/tmp/l"))
            WasiFileSystem.symlink(Path("a/b/c/d/e"), Path("/tmp/lr"))
            WasiFileSystem.symlink(Path("/blablabla"), Path("/tmp/dangling"))

            assertEquals(Path("/tmp/a/b"), SystemFileSystem.resolve(Path("/tmp/lr/../../..")))
            assertEquals(Path("/tmp/a/b/c"), SystemFileSystem.resolve(Path("/tmp/l/../..")))

            assertFailsWith<FileNotFoundException> {
                SystemFileSystem.resolve(Path("/tmp/dangling"))
            }
        } finally {
            SystemFileSystem.delete(Path("/tmp/a/b/c/d/e"))
            SystemFileSystem.delete(Path("/tmp/a/b/c/d"))
            SystemFileSystem.delete(Path("/tmp/a/b/c"))
            SystemFileSystem.delete(Path("/tmp/a/b"))
            SystemFileSystem.delete(Path("/tmp/a"))
            SystemFileSystem.delete(Path("/tmp/l"))
            SystemFileSystem.delete(Path("/tmp/lr"))
            SystemFileSystem.delete(Path("/tmp/dangling"))
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun symlinks() {
        val src = Path("/tmp/src")
        val tgt = Path("/tmp/tgt")

        try {
            SystemFileSystem.sink(src).buffered().use {
                it.writeString("Hello")
            }
            WasiFileSystem.symlink(src, tgt)
            SystemFileSystem.source(tgt).buffered().use {
                assertEquals("Hello", it.readLine())
            }
        } finally {
            SystemFileSystem.delete(src)
            SystemFileSystem.delete(tgt)
        }
    }

    @Test
    fun recursiveSimlinks() {
        val src = Path("/tmp/src")
        val tgt = Path("/tmp/tgt")

        WasiFileSystem.symlink(src, tgt)
        WasiFileSystem.symlink(tgt, src)

        try {
            assertFailsWith<IOException> {
                SystemFileSystem.resolve(src)
            }
        } finally {
            SystemFileSystem.delete(src)
            SystemFileSystem.delete(tgt)
        }
    }

    @Test
    fun deepSymlinksChain() {
        val src = Path("/tmp/src")

        SystemFileSystem.sink(src).close()

        val linksCount = 100
        val safeSymlinksDepth = 30
        val paths = mutableListOf(src)
        for (i in 0 ..< linksCount) {
            val link = Path("/tmp/link$i")
            WasiFileSystem.symlink(paths.last(), link)
            paths.add(link)
        }

        try {
            assertEquals(src, SystemFileSystem.resolve(paths[safeSymlinksDepth]))
            val exception = assertFailsWith<IOException> {
                SystemFileSystem.resolve(paths.last())
            }
            assertEquals("Too many levels of symbolic links", exception.message)
        } finally {
            paths.asReversed().forEach {
                SystemFileSystem.delete(it)
            }
        }
    }

    @Test
    fun multilevelSymlinks() {
        val src = Path("/tmp/result/a/z")

        // /tmp/x/y/z -> /tmp/foo/a/z -> /tmp/result/a/z
        val finalLink = Path("/tmp/x/y/z")

        try {
            SystemFileSystem.createDirectories(Path("/tmp/result/a"))
            SystemFileSystem.createDirectories(Path("/tmp/x"))
            // create a file
            SystemFileSystem.sink(src).close()

            WasiFileSystem.symlink(Path("/tmp/result"), Path("/tmp/foo"))
            WasiFileSystem.symlink(Path("/tmp/foo/a"), Path("/tmp/x/y"))

            assertEquals(src, WasiFileSystem.resolve(Path(finalLink)))
        } finally {
            SystemFileSystem.delete(Path("/tmp/x/y"))
            SystemFileSystem.delete(Path("/tmp/x"))
            SystemFileSystem.delete(Path("/tmp/foo"))
            SystemFileSystem.delete(Path("/tmp/result/a/z"))
            SystemFileSystem.delete(Path("/tmp/result/a"))
            SystemFileSystem.delete(Path("/tmp/result"))
        }
    }

    @Test
    fun mkdirRoot() {
        SystemFileSystem.createDirectories(Path("/tmp")) // should succeed
        assertFailsWith<IOException> { SystemFileSystem.createDirectories(Path("/tmp"), true) }
    }

    @Test
    fun intermediateDirectoryCreationFailure() {
        val targetPath = Path("/tmp/a/b/c/d/e")
        val existingFile = Path("/tmp/a/b")

        SystemFileSystem.createDirectories(Path("/tmp/a/"))
        try {
            SystemFileSystem.sink(existingFile).close()
            val exception = assertFailsWith<IOException> {
                SystemFileSystem.createDirectories(targetPath)
            }
            assertNotNull(exception.message)
            assertTrue(
                exception.message!!.startsWith(
                    "Can't create directory $targetPath. Creation of an intermediate directory /tmp/a/b/c failed"
                )
            )
        } finally {
            SystemFileSystem.delete(Path("/tmp/a/b"))
            SystemFileSystem.delete(Path("/tmp/a"))
        }
    }
}
