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
 * Compares the common readString() UTF-8 decoder against the JVM readString(charset) overload
 * across payload sizes, to pick the JDK version gate for the multi-release JAR specialization
 * discussed in https://github.com/Kotlin/kotlinx-io/issues/515.
 *
 * Payloads are ASCII JSON sliced to the exact byte count; "-mixed" additionally contains
 * 2-3 byte characters and an emoji. Both benchmarks include identical Buffer staging cost.
 */
@State(Scope.Benchmark)
open class ReadStringUtf8GateBenchmark {
    @Param("16", "128", "960", "16384", "102400", "16384-mixed")
    var payload: String = ""

    private var bytes: ByteArray = ByteArray(0)

    @Setup
    fun setup() {
        val size = payload.removeSuffix("-mixed").toInt()
        bytes = if (payload.endsWith("-mixed")) mixedJson(size) else asciiJson(size)
    }

    @Benchmark
    fun common(): String = Buffer().apply { write(bytes) }.readString()

    @Benchmark
    fun jvmOverload(): String = Buffer().apply { write(bytes) }.readString(Charsets.UTF_8)
}

private fun jsonObject(id: Int): String = buildString {
    append("{\"id\":").append(id)
    for (f in 1..29) {
        append(",\"field_").append(f).append("\":")
        when (f % 4) {
            0 -> append(id * 31 + f)
            1 -> append("\"value_").append(id).append('_').append(f).append('"')
            2 -> append((id + f) % 2 == 0)
            else -> append(id % 100).append('.').append(f)
        }
    }
    append('}')
}

private fun asciiJson(size: Int): ByteArray {
    val sb = StringBuilder("[")
    var id = 0
    while (sb.length <= size) {
        if (id > 0) sb.append(',')
        sb.append(jsonObject(id++))
    }
    return sb.substring(0, size).encodeToByteArray()
}

private fun mixedJson(size: Int): ByteArray {
    val sb = StringBuilder()
    var byteLength = 0
    var id = 0
    while (byteLength < size) {
        val chunk = "{\"città\":\"München_$id\",\"note\":\"日本語テキスト 🚀\"," +
                jsonObject(id).removePrefix("{") + ","
        sb.append(chunk)
        byteLength += chunk.encodeToByteArray().size
        id++
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
