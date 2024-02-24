/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.buffer

/**
 * Partial declaration of a class mirroring [node:buffer.Buffer](https://nodejs.org/api/buffer.html#buffer).
 */
internal expect class Buffer {
    val length: Int
    fun readInt8(offset: Int): Byte
    fun writeInt8(value: Byte, offset: Int)
}

internal expect fun allocUnsafe(bytes: Int): Buffer
