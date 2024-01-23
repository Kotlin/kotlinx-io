/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.wasi

import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

@OptIn(UnsafeWasmMemoryApi::class)
internal fun Pointer.readByteArray(length: Int): ByteArray {
    val out = ByteArray(length)

    for (idx in 0 until length) {
        out[idx] = (this + idx).loadByte()
    }

    return out
}
