/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.internal.commonAsUtf8ToByteArray

internal actual fun String.asUtf8ToByteArray(): ByteArray = commonAsUtf8ToByteArray()

public actual open class IOException actual constructor(
    message: String?,
    cause: Throwable?
) : Exception(message, cause) {
    public actual constructor(message: String?) : this(message, null)

    public actual constructor() : this(null)
}

public actual open class EOFException actual constructor(message: String?) : IOException(message) {
    public actual constructor() : this(null)
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias CommonJsModule = JsModule

@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias CommonJsNonModule = JsNonModule

internal actual fun withCaughtException(block: () -> Unit): Throwable? {
    try {
        block()
        return null
    } catch (t: Throwable) {
        return t
    }
}
