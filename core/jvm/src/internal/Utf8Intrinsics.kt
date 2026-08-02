/*
 * Copyright 2017-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.internal

internal actual fun decodeUtf8(source: ByteArray, beginIndex: Int, endIndex: Int): String =
    Utf8Intrinsics.decodeUtf8ToString(source, beginIndex, endIndex)

/**
 * UTF-8 decoding implementation selected through the multi-release jar mechanism.
 *
 * This is the base variant loaded on JDK 8 and Android; it delegates to the common decoder.
 * On JDK 9 and newer runtimes, the class of the same name from `META-INF/versions/9`
 * (compiled from the `jvm/src9` source root) is loaded instead.
 *
 * Both variants must declare identical signatures.
 */
internal object Utf8Intrinsics {
    fun decodeUtf8ToString(source: ByteArray, beginIndex: Int, endIndex: Int): String =
        source.commonToUtf8String(beginIndex, endIndex)
}
