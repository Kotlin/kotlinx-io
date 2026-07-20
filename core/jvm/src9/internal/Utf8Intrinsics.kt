/*
 * Copyright 2017-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.internal

/**
 * JDK 9+ variant of `Utf8Intrinsics`, packed into `META-INF/versions/9` of the multi-release jar
 * and shadowing the base variant from `jvm/src` on modern runtimes.
 *
 * Decoding through the `String` constructor engages the JDK's intrinsified UTF-8 decoder,
 * which scans and copies ASCII-dominant content in bulk instead of decoding it byte by byte
 * (3-4x faster and half the allocations for typical JSON/HTML payloads, see
 * https://github.com/Kotlin/kotlinx-io/issues/515). For ill-formed sequences it follows
 * the Unicode "maximal subpart" convention, which may substitute a different number of
 * replacement characters than the common decoder.
 *
 * Signatures must stay identical to the base variant.
 */
internal object Utf8Intrinsics {
    fun decodeUtf8ToString(source: ByteArray, beginIndex: Int, endIndex: Int): String =
        String(source, beginIndex, endIndex - beginIndex, Charsets.UTF_8)
}
