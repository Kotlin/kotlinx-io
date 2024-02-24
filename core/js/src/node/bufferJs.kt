/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.buffer

import kotlinx.io.node.loadModule

private val bufferModule: dynamic by lazy {
    loadModule("buffer") { js("require('buffer')") }
}

/**
 * Partial declaration of a class mirroring [node:buffer.Buffer](https://nodejs.org/api/buffer.html#buffer).
 */
internal actual class Buffer(internal val buffer: dynamic) {
    actual val length: Int
        get() = buffer.length as Int

    actual fun readInt8(offset: Int): Byte = buffer.readInt8(offset) as Byte
    actual fun writeInt8(value: Byte, offset: Int) {
        buffer.writeInt8(value, offset)
    }
}

internal actual fun allocUnsafe(bytes: Int): Buffer {
    return Buffer(bufferModule.Buffer.allocUnsafe(bytes))
}

