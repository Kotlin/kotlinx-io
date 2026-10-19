/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmInterop::class, UnsafeWasmMemoryApi::class)

package kotlinx.io

import kotlinx.io.files.*
import kotlinx.io.wasi.Errno
import kotlin.random.Random
import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.*

actual fun tempFileName(): String =
    Path(SystemTemporaryDirectory, Random.nextBytes(32).toHexString()).toString()

/**
 * Returns a value of the environment variable with the given [name], or null if it is absent.
 */
internal fun getEnv(name: String): String? = withScopedMemoryAllocator { allocator ->
    val countPtr = allocator.allocateInt()
    val sizePtr = allocator.allocateInt()

    environ_sizes_get(countPtr.address.toInt(), sizePtr.address.toInt()).also {
        val res = Errno(it)
        check(res == Errno.success) { "environ_sizes_get failed: ${res.description}" }
    }

    val count = countPtr.loadInt()
    if (count == 0) return@withScopedMemoryAllocator null

    // i'th entry is a pointer to the i'th env variable in the buffer,
    // represented as a null-terminated "var=value" string.
    val pointers = allocator.allocate(count * Int.SIZE_BYTES)
    // k/v pairs
    val buffer = allocator.allocate(sizePtr.loadInt())
    environ_get(pointers.address.toInt(), buffer.address.toInt()).also {
        val res = Errno(it)
        check(res == Errno.success) { "environ_get failed: ${res.description}" }
    }

    val bytes = buffer.loadBytes(sizePtr.loadInt())
    val prefix = "$name="
    repeat(count) { index ->
        val startOffset = pointers.loadInt(index * Int.SIZE_BYTES) - buffer.address.toInt()
        val endOffset = bytes.indexOfOrSize(0, startOffset)

        val kvPair = bytes.decodeToString(startOffset, endOffset)

        if (kvPair.startsWith(prefix)) {
            return@withScopedMemoryAllocator kvPair.substring(prefix.length)
        }
    }
    null
}

private fun ByteArray.indexOfOrSize(value: Byte, fromIndex: Int = 0): Int {
    for (idx in fromIndex until size) {
        if (this[idx] == value) return idx
    }
    return size
}

@WasmImport("wasi_snapshot_preview1", "environ_sizes_get")
private external fun environ_sizes_get(countPtr: Int, sizePtr: Int): Int

@WasmImport("wasi_snapshot_preview1", "environ_get")
private external fun environ_get(environPtr: Int, bufferPtr: Int): Int
