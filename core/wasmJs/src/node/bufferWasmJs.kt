/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.buffer

import kotlinx.io.node.loadModule

private val bufferModule by lazy {
    loadModule("buffer")
}

/**
 * Partial declaration of a class mirroring [node:buffer.Buffer](https://nodejs.org/api/buffer.html#buffer).
 */
internal actual class Buffer(internal val buffer: JsAny) {
    actual val length: Int
        get() = bufferLength(buffer)

    actual fun readInt8(offset: Int): Byte = readInt8(offset, buffer)

    actual fun writeInt8(value: Byte, offset: Int) {
        writeInt8(value, offset, buffer)
    }
}
internal actual fun allocUnsafe(bytes: Int): Buffer = Buffer(allocUnsafe(bytes, bufferModule))

private fun allocUnsafe(bytes: Int, mod: JsAny): JsAny = js("mod.Buffer.allocUnsafe(bytes)")
private fun bufferLength(buffer: JsAny): Int = js("buffer.length")
private fun readInt8(offset: Int, buffer: JsAny): Byte = js("buffer.readInt8(offset)")
private fun writeInt8(value: Byte, offset: Int, buffer: JsAny): Unit = js("buffer.writeInt8(value, offset)")
