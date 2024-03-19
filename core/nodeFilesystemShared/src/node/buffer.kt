/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

import kotlinx.io.withCaughtException

internal external interface BufferModule {
    val Buffer: BufferObj
}

internal external object BufferObj {
    fun allocUnsafe(bytes: Int): Buffer
}

/**
 * Partial declaration of a class mirroring [node:buffer.Buffer](https://nodejs.org/api/buffer.html#buffer).
 */
internal external interface Buffer {
    val length: Int
    fun readInt8(offset: Int): Byte
    fun writeInt8(value: Byte, offset: Int)
}

internal val buffer: BufferModule by lazy {
    loadModule("buffer", ::bufferInitializer)
}

private fun bufferInitializer(): BufferModule? = js("eval('require')('buffer')")
