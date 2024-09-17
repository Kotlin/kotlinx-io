/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.bytestring

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

/**
 * Returns a new [Int8Array] instance initialized with bytes copied from [this] ByteString.
 */
public fun ByteString.toInt8Array(): Int8Array {
    val res = Int8Array(size)
    for (idx in this.indices) {
        res[idx] = this[idx]
    }
    return res
}

/**
 * Returns a new [ArrayBuffer] instance initialized with bytes copied from [this] ByteString.
 */
public fun ByteString.toArrayBuffer(): ArrayBuffer = toInt8Array().buffer

/**
 * Return a new [ByteString] holding data copied from [this] Int8Array.
 */
public fun Int8Array.toByteString(): ByteString {
    if (length == 0) {
        return ByteString.EMPTY
    }
    val builder = ByteStringBuilder(length)
    for (idx in 0..<length) {
        builder.append(this[idx])
    }
    return builder.toByteString()
}

/**
 * Returns a new [ByteString] holding data copied from [this] ArrayBuffer
 */
public fun ArrayBuffer.toByteString(): ByteString = Int8Array(this).toByteString()