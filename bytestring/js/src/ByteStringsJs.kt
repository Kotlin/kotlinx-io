/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.bytestring

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

/**
 * Returns a new [Int8Array] instance initialized with bytes copied from [this] ByteString.
 */
public fun ByteString.toInt8Array(): Int8Array = toByteArray().unsafeCast<Int8Array>()

/**
 * Returns a new [ArrayBuffer] instance initialized with bytes copied from [this] ByteString.
 */
public fun ByteString.toArrayBuffer(): ArrayBuffer = toInt8Array().buffer

/**
 * Return a new [ByteString] holding data copied from [this] Int8Array.
 */
public fun Int8Array.toByteString(): ByteString = ByteString(*unsafeCast<ByteArray>())

/**
 * Returns a new [ByteString] holding data copied from [this] ArrayBuffer
 */
public fun ArrayBuffer.toByteString(): ByteString = Int8Array(this).toByteString()