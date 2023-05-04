/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

/**
 * This code is based on (and mostly duplicates) Okio BufferUtf8Benchmark:
 * https://github.com/square/okio/blob/master/okio/jvm/jmh/src/jmh/java/com/squareup/okio/benchmarks/BufferUtf8Benchmark.java
 */
package kotlinx.io.benchmark

import kotlinx.benchmark.*
import kotlinx.io.Buffer

@State(Scope.Benchmark)
open class BufferUtf8Benchmark {
    private val strings = mapOf(
    "ascii" to
    "Um, I'll tell you the problem with the scientific power that you're using here, "
    + "it didn't require any discipline to attain it. You read what others had done and you "
    + "took the next step. You didn't earn the knowledge for yourselves, so you don't take any "
    + "responsibility for it. You stood on the shoulders of geniuses to accomplish something "
    + "as fast as you could, and before you even knew what you had, you patented it, and "
    + "packaged it, and slapped it on a plastic lunchbox, and now you're selling it, you wanna "
    + "sell it.",
    "utf8" to
    "Սｍ, I'll 𝓽𝖾ll ᶌօ𝘂 ᴛℎ℮ 𝜚𝕣०ｂl𝖾ｍ ｗі𝕥𝒽 𝘵𝘩𝐞 𝓼𝙘𝐢𝔢𝓷𝗍𝜄𝚏𝑖ｃ 𝛠𝝾ｗ𝚎𝑟 𝕥ｈ⍺𝞃 𝛄𝓸𝘂'𝒓𝗲 υ𝖘𝓲𝗇ɡ 𝕙𝚎𝑟ｅ, "
    + "𝛊𝓽 ⅆ𝕚𝐝𝝿'𝗍 𝔯𝙚𝙦ᴜ𝜾𝒓𝘦 𝔞𝘯𝐲 ԁ𝜄𝑠𝚌ι𝘱lι𝒏ｅ 𝑡𝜎 𝕒𝚝𝖙𝓪і𝞹 𝔦𝚝. 𝒀ο𝗎 𝔯𝑒⍺𝖉 ｗ𝐡𝝰𝔱 𝞂𝞽һ𝓮𝓇ƽ հ𝖺𝖉 ⅾ𝛐𝝅ⅇ 𝝰πԁ 𝔂ᴑᴜ 𝓉ﮨ၀𝚔 "
    + "т𝒽𝑒 𝗇𝕖ⅹ𝚝 𝔰𝒕е𝓅. 𝘠ⲟ𝖚 𝖉ⅰԁ𝝕'τ 𝙚𝚊ｒ𝞹 𝘵Ꮒ𝖾 𝝒𝐧هｗl𝑒𝖉ƍ𝙚 𝓯૦ｒ 𝔂𝞼𝒖𝕣𝑠𝕖l𝙫𝖊𝓼, 𐑈о ｙ𝘰𝒖 ⅆە𝗇'ｔ 𝜏α𝒌𝕖 𝛂𝟉ℽ "
    + "𝐫ⅇ𝗌ⲣ๐ϖ𝖘ꙇᖯ𝓲l𝓲𝒕𝘆 𝐟𝞼𝘳 𝚤𝑡. 𝛶𝛔𝔲 ｓ𝕥σσ𝐝 ﮩ𝕟 𝒕𝗁𝔢 𝘴𝐡𝜎ᴜlⅾ𝓮𝔯𝚜 𝛐𝙛 ᶃ𝚎ᴨᎥս𝚜𝘦𝓈 𝓽𝞸 ａ𝒄𝚌𝞸ｍρl𝛊ꜱ𝐡 𝓈𝚘ｍ𝚎𝞃𝔥⍳𝞹𝔤 𝐚𝗌 𝖋ａ𝐬𝒕 "
    + "αｓ γ𝛐𝕦 𝔠ﻫ𝛖lԁ, 𝚊π𝑑 Ь𝑒𝙛૦𝓇𝘦 𝓎٥𝖚 ⅇｖℯ𝝅 𝜅ո𝒆ｗ ｗ𝗵𝒂𝘁 ᶌ੦𝗎 ｈ𝐚𝗱, 𝜸ﮨ𝒖 𝓹𝝰𝔱𝖾𝗇𝓽𝔢ⅆ і𝕥, 𝚊𝜛𝓭 𝓹𝖺ⅽϰ𝘢ℊеᏧ 𝑖𝞃, "
    + "𝐚𝛑ꓒ 𝙨l𝔞р𝘱𝔢𝓭 ɩ𝗍 ہ𝛑 𝕒 ｐl𝛂ѕᴛ𝗂𝐜 l𝞄ℼ𝔠𝒽𝑏ﮪ⨯, 𝔞ϖ𝒹 ｎ𝛔ｗ 𝛾𝐨𝞄'𝗿𝔢 ꜱ℮ll𝙞ｎɡ ɩ𝘁, 𝙮𝕠𝛖 ｗ𝑎ℼ𝚗𝛂 𝕤𝓮ll 𝙞𝓉.",
    // The first 't' is actually a '𝓽'
    "sparse" to
    "Um, I'll 𝓽ell you the problem with the scientific power that you're using here, "
    + "it didn't require any discipline to attain it. You read what others had done and you "
    + "took the next step. You didn't earn the knowledge for yourselves, so you don't take any "
    + "responsibility for it. You stood on the shoulders of geniuses to accomplish something "
    + "as fast as you could, and before you even knew what you had, you patented it, and "
    + "packaged it, and slapped it on a plastic lunchbox, and now you're selling it, you wanna "
    + "sell it.",
    "2bytes" to "\u0080\u07ff",
    "3bytes" to "\u0800\ud7ff\ue000\uffff",
    "4bytes" to "\ud835\udeca",
    // high surrogate, 'a', low surrogate, and 'a'
    "bad" to "\ud800\u0061\udc00\u0061")

    private val buffer = Buffer()

    @Param("20", "2000", "200000")
    var length = 0

    @Param("ascii", "utf8", "sparse", "2bytes", "3bytes", "4bytes", "bad")
    var encoding: String = "ascii"

    private var string: String = ""

    @Setup
    fun setupString() {
        val part = strings[encoding]
        val builder = StringBuilder(length)
        while (builder.length < length) {
            builder.append(part)
        }
        builder.setLength(length)
        string = builder.toString()
    }

    @Benchmark
    fun readWrite() = buffer.writeUtf8(string).readUtf8()

    @Benchmark
    fun write() {
        buffer.writeUtf8(string)
        buffer.clear()
    }
}