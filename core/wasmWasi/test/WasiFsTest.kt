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
        val preopen = PreOpens.preopens.single()
        assertEquals(3, preopen.fd)
        assertEquals(Path("/tmp"), preopen.path)
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
