/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.browser.window
import kotlinx.io.node.os

public actual open class IOException : Exception {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public actual constructor(cause: Throwable?) : super(cause)

    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

public actual open class EOFException : IOException {
    public actual constructor() : super()

    public actual constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)
}

internal actual fun withCaughtException(block: () -> Unit): Throwable? {
    try {
        block()
        return null
    } catch (t: Throwable) {
        return t
    }
}

/**
 * Sequence of characters used as a line separator by the underlying platform.
 *
 * In NodeJS-compatible environments, this property derives value from [os.EOL](https://nodejs.org/api/os.html#oseol).
 * In all other environments, it checks [Navigator.platform](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/platform)
 * property and returns `"\r\n"` if the platform is Windows, and `"\n"` otherwise.
 */
public actual val SystemLineSeparator: String by lazy {
    try {
        os.EOL
    } catch (_: Throwable) {
        platformLineSeparator()
    }
}

private fun platformLineSeparator(): String {
    val platform = window.navigator.platform
    return if (platform.startsWith("Win", ignoreCase = true)) "\r\n" else "\n"
}
