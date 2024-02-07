/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
}
