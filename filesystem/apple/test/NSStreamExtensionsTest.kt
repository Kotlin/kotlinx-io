/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.Buffer
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.tempFileName
import kotlinx.io.writeString
import platform.Foundation.NSInputStream
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertEquals

class NSStreamExtensionsTest {
    @Test
    fun nsInputStreamSourceFromFile() {
        val file = tempFileName()
        try {
            SystemFileSystem.sink(Path(file)).buffered().use {
                it.writeString("example")
            }

            val input = NSInputStream(uRL = NSURL.fileURLWithPath(file))
            val source = input.asSource()
            val buffer = Buffer()
            assertEquals(7, source.readAtMostTo(buffer, 10))
            assertEquals("example", buffer.readString())
        } finally {
            SystemFileSystem.delete(Path(file))
        }
    }
}
