/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.io.Buffer
import kotlinx.io.readString

/**
 * Measures readString() over a fixed 16 KiB payload while sweeping the density of
 * non-ASCII characters, to locate the crossover between the JDK decoder's bulk-ASCII
 * advantage and its cost on scattered multi-byte content.
 *
 * Kinds: "ascii" is a pure-ASCII baseline; "latin-N" replaces N% of characters with
 * a 2-byte char (U+00E9, stays Latin-1-compact on the JVM); "cjk-N" uses a 3-byte
 * char (U+65E5, forces UTF-16 storage). Characters are scattered evenly.
 */
@State(Scope.Benchmark)
open class Utf8DensitySweepBenchmark {
    @Param("ascii", "latin-2", "latin-10", "latin-33", "latin-100", "cjk-2", "cjk-10", "cjk-33", "cjk-100")
    var kind: String = ""

    private var bytes: ByteArray = ByteArray(0)

    @Setup
    fun setup() {
        bytes = sweepPayload(kind, 16384)
    }

    @Benchmark
    fun readString(): String = Buffer().apply { write(bytes) }.readString()
}

private fun sweepPayload(kind: String, size: Int): ByteArray {
    fun baseChar(i: Int): Char = if (i % 8 == 7) ' ' else ('a' + (i * 7) % 26)

    val everyNth: Int
    val replacement: Char
    if (kind == "ascii") {
        everyNth = 0
        replacement = ' '
    } else {
        val parts = kind.split('-')
        replacement = if (parts[0] == "latin") 'é' else '日'
        everyNth = 100 / parts[1].toInt()
    }

    val sb = StringBuilder()
    var byteLength = 0
    var i = 0
    while (byteLength < size) {
        val c = if (everyNth > 0 && i % everyNth == 0) replacement else baseChar(i)
        sb.append(c)
        byteLength += if (c.code < 0x80) 1 else if (c.code < 0x800) 2 else 3
        i++
    }
    var s = sb.toString()
    var encoded = s.encodeToByteArray()
    while (encoded.size > size) {
        s = s.substring(0, s.length - 1)
        encoded = s.encodeToByteArray()
    }
    // Pad with spaces at the byte level to hit the exact size; the result stays valid UTF-8.
    val out = encoded.copyOf(size)
    out.fill(' '.code.toByte(), encoded.size, size)
    return out
}
