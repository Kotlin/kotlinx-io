/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.*

@State(Scope.Benchmark)
abstract class BufferRWBenchmarkBase {
    // Buffers are implemented as list of segments, as soon as a segment is empty
    // it will be unlinked. By reading all previously written data, a segment will be
    // cleared and recycled and the next time we will try to write data a new segment
    // will be requested from the pool. Thus, without having some data in-flight we will
    // benchmark not only read/write ops performance, but also segments allocation/reclamation.
    @Param("128")
    var minGap: Int = 0

    protected val buffer = Buffer()
    protected val okioBuffer = okio.Buffer()

    protected open fun padding(): ByteArray = ByteArray(minGap)

    @Setup
    fun setupBuffers() {
        val padding = padding()
        buffer.write(padding)
        okioBuffer.write(padding)
    }

    @TearDown()
    fun clearBuffers() {
        okioBuffer.clear()
        buffer.clear()
    }
}

open class ByteBenchmark: BufferRWBenchmarkBase() {
    @Benchmark
    fun kio(): Byte {
        buffer.writeByte(0x42)
        return buffer.readByte()
    }

    @Benchmark
    fun okio(): Byte {
        okioBuffer.writeByte(0x42)
        return okioBuffer.readByte()
    }
}

open class ShortBenchmark: BufferRWBenchmarkBase() {
    @Benchmark
    fun kio(): Short {
        buffer.writeShort(42)
        return buffer.readShort()
    }

    @Benchmark
    fun okio(): Short {
        okioBuffer.writeShort(42)
        return okioBuffer.readShort()
    }
}

open class IntBenchmark: BufferRWBenchmarkBase() {
    @Benchmark
    fun kio(): Int {
        buffer.writeInt(42)
        return buffer.readInt()
    }

    @Benchmark
    fun okio(): Int {
        okioBuffer.writeInt(42)
        return okioBuffer.readInt()
    }
}

open class LongBenchmark: BufferRWBenchmarkBase() {
    @Benchmark
    fun kio(): Long {
        buffer.writeLong(42)
        return buffer.readLong()
    }

    @Benchmark
    fun okio(): Long {
        okioBuffer.writeLong(42)
        return okioBuffer.readLong()
    }
}

open class ShortLeBenchmark: BufferRWBenchmarkBase() {
    @Benchmark
    fun kio(): Short {
        buffer.writeShortLe(42)
        return buffer.readShortLe()
    }

    @Benchmark
    fun okio(): Short {
        okioBuffer.writeShortLe(42)
        return okioBuffer.readShortLe()
    }
}

open class IntLeBenchmark: BufferRWBenchmarkBase() {
    @Benchmark
    fun kio(): Int {
        buffer.writeIntLe(42)
        return buffer.readIntLe()
    }

    @Benchmark
    fun okio(): Int {
        okioBuffer.writeIntLe(42)
        return okioBuffer.readIntLe()
    }
}

open class LongLeBenchmark: BufferRWBenchmarkBase() {
    @Benchmark
    fun kio(): Long {
        buffer.writeLongLe(42)
        return buffer.readLongLe()
    }

    @Benchmark
    fun okio(): Long {
        okioBuffer.writeLongLe(42)
        return okioBuffer.readLongLe()
    }
}

open class DecimalLongBenchmark: BufferRWBenchmarkBase() {
    @Param("-9223372036854775806", "9223372036854775806", "1")
    var value = 0L

    override fun padding(): ByteArray {
        val tmpBuffer = Buffer()
        while (tmpBuffer.size < minGap) {
            tmpBuffer.writeDecimalLong(value).writeByte(' '.code)
        }
        return tmpBuffer.readByteArray()
    }

    @Benchmark
    fun kio(): Long {
        buffer.writeDecimalLong(value).writeByte(' '.code)
        val l = buffer.readDecimalLong()
        buffer.readByte()
        return l
    }

    @Benchmark
    fun okio(): Long {
        okioBuffer.writeDecimalLong(value).writeByte(' '.code)
        val l = okioBuffer.readDecimalLong()
        okioBuffer.readByte()
        return l
    }
}

open class HexadecimalLongBenchmark: BufferRWBenchmarkBase() {
    @Param("9223372036854775806", "1")
    var value = 0L

    override fun padding(): ByteArray {
        val tmpBuffer = Buffer()
        while (tmpBuffer.size < minGap) {
            tmpBuffer.writeHexadecimalUnsignedLong(value).writeByte(' '.code)
        }
        return tmpBuffer.readByteArray()
    }

    @Benchmark
    fun kio(): Long {
        buffer.writeHexadecimalUnsignedLong(value).writeByte(' '.code)
        val l = buffer.readHexadecimalUnsignedLong()
        buffer.readByte()
        return l
    }

    @Benchmark
    fun okio(): Long {
        okioBuffer.writeHexadecimalUnsignedLong(value).writeByte(' '.code)
        val l = okioBuffer.readHexadecimalUnsignedLong()
        okioBuffer.readByte()
        return l
    }
}

open class Utf8CodePointBenchmark: BufferRWBenchmarkBase() {
    @Param("1", "2", "3")
    var bytes: Int = 0

    var codePoint: Int = 0

    @Setup
    fun setupCodePoints() {
        codePoint = when (bytes) {
            1 -> 'a'.code
            2 -> 'ɐ'.code
            3 -> 'ა'.code
            else -> throw IllegalArgumentException()
        }
    }

    @Benchmark
    fun kio(): Int {
        buffer.writeUtf8CodePoint(codePoint)
        return buffer.readUtf8CodePoint()
    }

    @Benchmark
    fun okio(): Int {
        okioBuffer.writeUtf8CodePoint(codePoint)
        return okioBuffer.readUtf8CodePoint()
    }
}

// This benchmark is based on https://raw.githubusercontent.com/square/okio/master/okio/jvm/jmh/src/jmh/java/com/squareup/okio/benchmarks/BufferUtf8Benchmark.java
open class Utf8StringBenchmark: BufferRWBenchmarkBase() {
    private val strings = mapOf(
        "ascii" to ("Um, I'll tell you the problem with the scientific power that you're using here, "
                + "it didn't require any discipline to attain it. You read what others had done and you "
                + "took the next step. You didn't earn the knowledge for yourselves, so you don't take any "
                + "responsibility for it. You stood on the shoulders of geniuses to accomplish something "
                + "as fast as you could, and before you even knew what you had, you patented it, and "
                + "packaged it, and slapped it on a plastic lunchbox, and now you're selling it, you wanna "
                + "sell it."),
        "utf8" to
            ("Սｍ, I'll 𝓽𝖾ll ᶌօ𝘂 ᴛℎ℮ 𝜚𝕣०ｂl𝖾ｍ ｗі𝕥𝒽 𝘵𝘩𝐞 𝓼𝙘𝐢𝔢𝓷𝗍𝜄𝚏𝑖ｃ 𝛠𝝾ｗ𝚎𝑟 𝕥ｈ⍺𝞃 𝛄𝓸𝘂'𝒓𝗲 υ𝖘𝓲𝗇ɡ 𝕙𝚎𝑟ｅ, "
                    + "𝛊𝓽 ⅆ𝕚𝐝𝝿'𝗍 𝔯𝙚𝙦ᴜ𝜾𝒓𝘦 𝔞𝘯𝐲 ԁ𝜄𝑠𝚌ι𝘱lι𝒏ｅ 𝑡𝜎 𝕒𝚝𝖙𝓪і𝞹 𝔦𝚝. 𝒀ο𝗎 𝔯𝑒⍺𝖉 ｗ𝐡𝝰𝔱 𝞂𝞽һ𝓮𝓇ƽ հ𝖺𝖉 ⅾ𝛐𝝅ⅇ 𝝰πԁ 𝔂ᴑᴜ 𝓉ﮨ၀𝚔 "
                    + "т𝒽𝑒 𝗇𝕖ⅹ𝚝 𝔰𝒕е𝓅. 𝘠ⲟ𝖚 𝖉ⅰԁ𝝕'τ 𝙚𝚊ｒ𝞹 𝘵Ꮒ𝖾 𝝒𝐧هｗl𝑒𝖉ƍ𝙚 𝓯૦ｒ 𝔂𝞼𝒖𝕣𝑠𝕖l𝙫𝖊𝓼, 𐑈о ｙ𝘰𝒖 ⅆە𝗇'ｔ 𝜏α𝒌𝕖 𝛂𝟉ℽ "
                    + "𝐫ⅇ𝗌ⲣ๐ϖ𝖘ꙇᖯ𝓲l𝓲𝒕𝘆 𝐟𝞼𝘳 𝚤𝑡. 𝛶𝛔𝔲 ｓ𝕥σσ𝐝 ﮩ𝕟 𝒕𝗁𝔢 𝘴𝐡𝜎ᴜlⅾ𝓮𝔯𝚜 𝛐𝙛 ᶃ𝚎ᴨᎥս𝚜𝘦𝓈 𝓽𝞸 ａ𝒄𝚌𝞸ｍρl𝛊ꜱ𝐡 𝓈𝚘ｍ𝚎𝞃𝔥⍳𝞹𝔤 𝐚𝗌 𝖋ａ𝐬𝒕 "
                    + "αｓ γ𝛐𝕦 𝔠ﻫ𝛖lԁ, 𝚊π𝑑 Ь𝑒𝙛૦𝓇𝘦 𝓎٥𝖚 ⅇｖℯ𝝅 𝜅ո𝒆ｗ ｗ𝗵𝒂𝘁 ᶌ੦𝗎 ｈ𝐚𝗱, 𝜸ﮨ𝒖 𝓹𝝰𝔱𝖾𝗇𝓽𝔢ⅆ і𝕥, 𝚊𝜛𝓭 𝓹𝖺ⅽϰ𝘢ℊеᏧ 𝑖𝞃, "
                    + "𝐚𝛑ꓒ 𝙨l𝔞р𝘱𝔢𝓭 ɩ𝗍 ہ𝛑 𝕒 ｐl𝛂ѕᴛ𝗂𝐜 l𝞄ℼ𝔠𝒽𝑏ﮪ⨯, 𝔞ϖ𝒹 ｎ𝛔ｗ 𝛾𝐨𝞄'𝗿𝔢 ꜱ℮ll𝙞ｎɡ ɩ𝘁, 𝙮𝕠𝛖 ｗ𝑎ℼ𝚗𝛂 𝕤𝓮ll 𝙞𝓉."),
        // The first 't' is actually a '𝓽'
        "sparse" to ("Um, I'll 𝓽ell you the problem with the scientific power that you're using here, "
                + "it didn't require any discipline to attain it. You read what others had done and you "
                + "took the next step. You didn't earn the knowledge for yourselves, so you don't take any "
                + "responsibility for it. You stood on the shoulders of geniuses to accomplish something "
                + "as fast as you could, and before you even knew what you had, you patented it, and "
                + "packaged it, and slapped it on a plastic lunchbox, and now you're selling it, you wanna "
                + "sell it."),
        "2bytes" to "\u0080\u07ff",
        "3bytes" to "\u0800\ud7ff\ue000\uffff",
        "4bytes" to "\ud835\udeca",
        // high surrogate, 'a', low surrogate, and 'a'
        "bad" to "\ud800\u0061\udc00\u0061")

    @Param("20", "2000", "200000")
    var length = 0

    @Param("ascii", "utf8", "sparse", "2bytes", "3bytes", "4bytes", "bad")
    var encoding: String = "ascii"

    private var string: String = ""

    private fun constructString(): String {
        val part = strings[encoding] ?: throw IllegalArgumentException("Unsupported encoding: $encoding")
        val builder = StringBuilder(length + 1000)
        while (builder.length < length) {
            builder.append(part)
        }
        builder.setLength(length)
        return builder.toString()
    }

    override fun padding(): ByteArray {
        val baseString = constructString()
        if (baseString.utf8Size() >= minGap) {
            return baseString.encodeToByteArray()
        }
        val builder = StringBuilder((minGap * 1.5).toInt())
        while (builder.length < minGap) {
            builder.append(baseString)
        }
        return builder.toString().encodeToByteArray()
    }

    @Setup
    fun setupString() {
        string = constructString()
    }

    @Benchmark
    fun kio(): String {
        buffer.writeUtf8(string)
        return buffer.readUtf8(length.toLong())
    }

    @Benchmark
    fun okio(): String {
        okioBuffer.writeUtf8(string)
        return okioBuffer.readUtf8(length.toLong())
    }
}

open class Utf8LineBenchmarkBase: BufferRWBenchmarkBase() {
    @Param("17")
    var length: Int = 0

    @Param("LF", "CRLF")
    var separator: String = ""

    protected var string: String = ""

    private fun lineSeparator(): String = when (separator) {
        "LF" -> "\n"
        "CRLF" -> "\r\n"
        else -> throw IllegalArgumentException("Unsupported line separator type: $separator")
    }

    private fun constructString(): String = ".".repeat(length) + lineSeparator()

    override fun padding(): ByteArray {
        val string = constructString()
        if (string.length >= minGap) {
            return string.encodeToByteArray()
        }
        val builder = StringBuilder((minGap * 1.5).toInt())
        while (builder.length < minGap) {
            builder.append(string)
        }
        return builder.toString().encodeToByteArray()
    }

    @Setup
    fun setupString() {
        string = constructString()
    }
}

open class Utf8LineBenchmark: Utf8LineBenchmarkBase() {
    @Benchmark
    fun kio(): String? {
        buffer.writeUtf8(string)
        return buffer.readUtf8Line()
    }

    @Benchmark
    fun okio(): String? {
        okioBuffer.writeUtf8(string)
        return okioBuffer.readUtf8Line()
    }
}

open class Utf8LineStrictBenchmark: Utf8LineBenchmarkBase() {
    @Benchmark
    fun kio(): String {
        buffer.writeUtf8(string)
        return buffer.readUtf8LineStrict()
    }

    @Benchmark
    fun okio(): String {
        okioBuffer.writeUtf8(string)
        return okioBuffer.readUtf8LineStrict()
    }
}

@State(Scope.Benchmark)
open class IndexOfBenchmark {
    @Param("128")
    var dataSize: Int = 0

    @Param("-1", "7", "100")
    var valueOffset: Int = 0

    @Param("true", "false")
    var spanOverMultipleSegment: Boolean = false

    private val buffer = Buffer()
    private val okioBuffer = okio.Buffer()

    @Setup
    fun setupBuffers() {
        val array = ByteArray(dataSize)
        if (valueOffset >= 0) array[valueOffset] = 1

        var paddingSize = 0L
        if (spanOverMultipleSegment) {
            paddingSize = 8192L - dataSize / 2L
        }
        val padding = ByteArray(paddingSize.toInt())
        buffer.write(padding).write(array).skip(paddingSize)
        okioBuffer.write(padding).write(array).skip(paddingSize)
    }

    @Benchmark
    fun kio(): Long = buffer.indexOf(1)

    @Benchmark
    fun okio(): Long = okioBuffer.indexOf(1)
}