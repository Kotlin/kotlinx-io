/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.internal.commonAsUtf8ToByteArray

internal actual fun String.asUtf8ToByteArray(): ByteArray = commonAsUtf8ToByteArray()

public actual open class IOException : Exception {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(cause: Throwable?) : super(cause)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

public actual open class EOFException : IOException {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)
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
