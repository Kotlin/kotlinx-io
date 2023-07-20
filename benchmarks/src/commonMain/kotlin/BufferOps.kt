/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.benchmarks

import kotlinx.benchmark.*
import kotlinx.io.*

@State(Scope.Benchmark)
abstract class BufferRWBenchmarkBase {
    // Buffers are implemented as a list of segments, as soon as a segment is empty
    // it will be unlinked. By reading all previously written data, a segment will be
    // cleared and recycled, and the next time we will try to write data, a new segment
    // will be requested from the pool. Thus, without having any data in-flight, we will
    // benchmark not only read/write ops performance, but also segments allocation/reclamation.
    // Specific non-zero minGap's values don't significantly affect overall results, but it is
    // left as the parameter to allow fine-tuning in the future.
    @Param("128")
    var minGap: Int = 0

    protected val buffer = Buffer()

    protected open fun padding(): ByteArray = ByteArray(minGap)

    @Setup
    fun setupBuffers() {
        val padding = padding()
        buffer.write(padding)
    }

    @TearDown
    fun clearBuffers() {
        buffer.clear()
    }
}

@State(Scope.Benchmark)
open class ByteBenchmark : BufferRWBenchmarkBase() {
    @Benchmark
    fun benchmark(): Byte {
        buffer.writeByte(0x42)
        return buffer.readByte()
    }
}

@State(Scope.Benchmark)
open class ShortBenchmark : BufferRWBenchmarkBase() {
    @Benchmark
    fun benchmark(): Short {
        buffer.writeShort(42)
        return buffer.readShort()
    }
}

@State(Scope.Benchmark)
open class IntBenchmark : BufferRWBenchmarkBase() {
    @Benchmark
    fun benchmark(): Int {
        buffer.writeInt(42)
        return buffer.readInt()
    }
}

@State(Scope.Benchmark)
open class LongBenchmark : BufferRWBenchmarkBase() {
    @Benchmark
    fun benchmark(): Long {
        buffer.writeLong(42)
        return buffer.readLong()
    }
}

@State(Scope.Benchmark)
open class ShortLeBenchmark : BufferRWBenchmarkBase() {
    @Benchmark
    fun benchmark(): Short {
        buffer.writeShortLe(42)
        return buffer.readShortLe()
    }
}

@State(Scope.Benchmark)
open class IntLeBenchmark : BufferRWBenchmarkBase() {
    @Benchmark
    fun benchmark(): Int {
        buffer.writeIntLe(42)
        return buffer.readIntLe()
    }
}

@State(Scope.Benchmark)
open class LongLeBenchmark : BufferRWBenchmarkBase() {
    @Benchmark
    fun benchmark(): Long {
        buffer.writeLongLe(42)
        return buffer.readLongLe()
    }
}

@State(Scope.Benchmark)
open class DecimalLongBenchmark : BufferRWBenchmarkBase() {
    @Param("-9223372036854775806", "9223372036854775806", "1")
    var value = 0L

    override fun padding(): ByteArray {
        return with(Buffer()) {
            while (size < minGap) {
                writeDecimalLong(value)
                // use space as a delimiter between consecutive decimal values
                writeByte(' '.code.toByte())
            }
            readByteArray()
        }
    }

    @Benchmark
    fun benchmark(): Long {
        // use space as a delimiter between consecutive decimal values
        buffer.writeDecimalLong(value)
        buffer.writeByte(' '.code.toByte())
        val l = buffer.readDecimalLong()
        buffer.readByte() // consume the delimiter
        return l
    }
}

@State(Scope.Benchmark)
open class HexadecimalLongBenchmark : BufferRWBenchmarkBase() {
    @Param("9223372036854775806", "1")
    var value = 0L

    override fun padding(): ByteArray {
        return with(Buffer()) {
            while (size < minGap) {
                writeHexadecimalUnsignedLong(value)
                writeByte(' '.code.toByte())
            }
            readByteArray()
        }
    }

    @Benchmark
    fun benchmark(): Long {
        buffer.writeHexadecimalUnsignedLong(value)
        buffer.writeByte(' '.code.toByte())
        val l = buffer.readHexadecimalUnsignedLong()
        buffer.readByte()
        return l
    }
}

// This benchmark is based on Okio benchmark:
// https://raw.githubusercontent.com/square/okio/master/okio/jvm/jmh/src/jmh/java/com/squareup/okio/benchmarks/BufferUtf8Benchmark.java
@State(Scope.Benchmark)
open class Utf8StringBenchmark : BufferRWBenchmarkBase() {
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
        "bad" to "\ud800\u0061\udc00\u0061"
    )

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
        val baseStringByteArray = baseString.encodeToByteArray()
        if (baseStringByteArray.size >= minGap) {
            return baseStringByteArray
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
    fun benchmark(): String {
        val s = buffer.size
        buffer.writeString(string)
        return buffer.readString(buffer.size - s)
    }
}

@State(Scope.Benchmark)
open class Utf8LineBenchmarkBase : BufferRWBenchmarkBase() {
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

@State(Scope.Benchmark)
open class Utf8LineBenchmark : Utf8LineBenchmarkBase() {
    @Benchmark
    fun benchmark(): String? {
        buffer.writeString(string)
        return buffer.readLine()
    }
}

@State(Scope.Benchmark)
open class Utf8LineStrictBenchmark : Utf8LineBenchmarkBase() {
    @Benchmark
    fun benchmark(): String {
        buffer.writeString(string)
        return buffer.readLineStrict()
    }
}

private const val VALUE_TO_FIND: Byte = 1

@State(Scope.Benchmark)
open class IndexOfBenchmark {
    @Param(
        "128:0:-1", // scan a short sequence at the beginning of a segment, target value is not there
        "128:0:7", // scan a short sequence at the beginning of a segment, target value in the beginning
        "128:0:100", // scan a short sequence at the beginning of a segment, target value at the end
        "128:" + (SEGMENT_SIZE_IN_BYTES - 64).toString() + ":100", // scan two consecutive segments
        (SEGMENT_SIZE_IN_BYTES * 3).toString() + ":0:-1" // scan multiple segments
    )
    var params: String = "0:0:-1";

    private val buffer = Buffer()

    @Setup
    fun setupBuffers() {
        val paramsParsed = params.split(':').map { it.toInt() }.toIntArray()
        check(paramsParsed.size == 3) {
            "Parameters format is: \"dataSize:paddingSize:valueIndex\", " +
                    "where valueIndex could be -1 if there should be no target value."
        }
        val dataSize = paramsParsed[0]
        val paddingSize = paramsParsed[1]
        val valueOffset = paramsParsed[2]
        check(paddingSize >= 0 && dataSize >= 0)
        check(valueOffset == -1 || valueOffset < dataSize)

        val array = ByteArray(dataSize)
        if (valueOffset >= 0) array[valueOffset] = VALUE_TO_FIND

        val padding = ByteArray(paddingSize)
        with(buffer) {
            write(padding)
            write(array)
            skip(paddingSize.toLong())
        }
    }

    @Benchmark
    fun benchmark(): Long = buffer.indexOf(VALUE_TO_FIND)
}

const val OFFSET_TO_2ND_BYTE_IN_2ND_SEGMENT = (SEGMENT_SIZE_IN_BYTES + 1).toString()

@State(Scope.Benchmark)
open class BufferGetBenchmark {
    private val buffer = Buffer()

    @Param("0", OFFSET_TO_2ND_BYTE_IN_2ND_SEGMENT)
    var offset: Long = 0

    @Setup
    fun fillBuffer() {
        buffer.write(ByteArray(offset.toInt() + 1))
    }

    @Benchmark
    fun get() = buffer[offset]
}

@State(Scope.Benchmark)
open class BufferReadWriteByteArray : BufferRWBenchmarkBase() {
    private var inputArray = ByteArray(0)
    private var outputArray = ByteArray(0)

    @Param("1", "1024", (SEGMENT_SIZE_IN_BYTES * 3).toString())
    var size: Int = 0

    @Setup
    fun allocateArrays() {
        inputArray = ByteArray(size)
        outputArray = ByteArray(size)
    }

    @Benchmark
    fun benchmark(blackhole: Blackhole) {
        buffer.write(inputArray)
        buffer.readTo(outputArray)
        blackhole.consume(outputArray)
    }
}

@State(Scope.Benchmark)
open class BufferReadNewByteArray : BufferRWBenchmarkBase() {
    private var inputArray = ByteArray(0)

    @Param("1", "1024", (SEGMENT_SIZE_IN_BYTES * 3).toString())
    var size: Int = 0

    @Setup
    fun allocateArray() {
        inputArray = ByteArray(size)
    }

    @Benchmark
    fun benchmark(): ByteArray {
        buffer.write(inputArray)
        return buffer.readByteArray(size)
    }
}
