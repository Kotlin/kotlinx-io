/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmark

import kotlinx.benchmark.*
import kotlinx.io.Buffer
import kotlinx.io.ByteString

@State(Scope.Benchmark)
open class MessageDigestBenchmark {
    private val buffer = Buffer()
    private var digestFunction: Buffer.() -> ByteString = {ByteString.EMPTY}

    @Param("0", "1", "16", "1024", "8196", "10000")
    var size: Int = 0

    @Param("MD5", "SHA-1", "SHA-256", "SHA-512")
    var algorithm: String = ""

    @Setup
    fun fillBuffer() {
        buffer.write(ByteArray(size) {it.toByte()})
        digestFunction = when (algorithm) {
            "MD5" -> Buffer::md5
            "SHA-1" -> Buffer::sha1
            "SHA-256" -> Buffer::sha256
            "SHA-512" -> Buffer::sha512
            else -> throw IllegalArgumentException("Unsupported algorithm: $algorithm")
        }
    }

    @Benchmark
    fun digest(): ByteString = digestFunction(buffer)
}