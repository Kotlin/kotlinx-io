/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
@file:OptIn(UnsafeWasmMemoryApi::class)

package kotlinx.io

import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi

internal fun Pointer.loadInt(offset: Int): Int = (this + offset).loadInt()
internal fun Pointer.loadLong(offset: Int): Long = (this + offset).loadLong()
internal fun Pointer.loadShort(offset: Int): Short = (this + offset).loadShort()
internal fun Pointer.loadByte(offset: Int): Byte = (this + offset).loadByte()

internal fun Pointer.loadBytes(length: Int): ByteArray {
    val buffer = ByteArray(length)
    for (offset in 0 until length) {
        buffer[offset] = this.loadByte(offset)
    }
    return buffer
}

internal fun Pointer.storeInt(offset: Int, value: Int): Unit = (this + offset).storeInt(value)
internal fun Pointer.storeLong(offset: Int, value: Long): Unit = (this + offset).storeLong(value)
internal fun Pointer.storeShort(offset: Int, value: Short): Unit = (this + offset).storeShort(value)
internal fun Pointer.storeByte(offset: Int, value: Byte): Unit = (this + offset).storeByte(value)

internal fun Pointer.storeBytes(bytes :ByteArray) {
    for (offset in bytes.indices) {
       this.storeByte(offset, bytes[offset])
    }
}

@OptIn(UnsafeWasmMemoryApi::class)
internal fun Buffer.readToLinearMemory(pointer: Pointer, bytes: Int) {
    checkBounds(size, 0L, bytes.toLong())
    var current: Segment? = head
    var remaining = bytes
    var currentPtr = pointer
    do {
        current!!
        val data = current.data
        val pos = current.pos
        val limit = current.limit
        val read = minOf(remaining, limit - pos)
        for (offset in 0 until read) {
            currentPtr.storeByte(offset, data[pos + offset])
        }
        currentPtr += read
        remaining -= read
        current = current.next
    } while (current != head && remaining > 0)
    check(remaining == 0)
    skip(bytes.toLong())
}


internal fun Buffer.writeFromLinearMemory(pointer: Pointer, bytes: Int) {
    var remaining = bytes
    var currentPtr = pointer
    while (remaining > 0) {
        val segment = writableSegment(1)
        val limit = segment.limit
        val data = segment.data
        val toWrite = minOf(data.size - limit, remaining)

        for (offset in 0 until toWrite) {
            data[limit + offset] = currentPtr.loadByte(offset)
        }

        currentPtr += toWrite
        remaining -= toWrite
        segment.limit += toWrite
        size += toWrite
    }
}

