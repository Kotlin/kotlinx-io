/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.StreamingTransformation
import kotlinx.io.UnsafeIoApi
import java.util.zip.Deflater

/**
 * A [StreamingTransformation] implementation that uses [java.util.zip.Deflater] for DEFLATE compression.
 */
@OptIn(UnsafeIoApi::class)
internal class DeflaterCompressor(
    private val deflater: Deflater
) : StreamingTransformation() {

    private var finishCalled = false

    override fun feedInput(source: ByteArray, startIndex: Int, endIndex: Int) {
        deflater.setInput(source, startIndex, endIndex - startIndex)
    }

    override fun needsInput(): Boolean = deflater.needsInput()

    override fun drainOutput(destination: ByteArray, startIndex: Int, endIndex: Int): Int {
        return deflater.deflate(destination, startIndex, endIndex - startIndex)
    }

    override fun finalizeOutput(destination: ByteArray, startIndex: Int, endIndex: Int): Int {
        if (!finishCalled) {
            deflater.finish()
            finishCalled = true
        }
        if (deflater.finished()) return -1
        return deflater.deflate(destination, startIndex, endIndex - startIndex)
    }

    override fun close() {
        deflater.end()
    }
}
