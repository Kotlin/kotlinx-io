/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

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
 * Value of this property depends on [Navigator.platform](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/platform):
 * it is `"\r\n"` on Windows, and `"\n"` on all other platforms.
 */
public actual val SystemLineSeparator: String by lazy {
    if (isWindows) {
        "\r\n"
    } else {
        "\n"
    }
}

internal actual val isWindows: Boolean by lazy {
    getPlatformName().startsWith("Win", ignoreCase = true)
}

private fun getPlatformName(): String = js(
    """
        (typeof navigator !== "undefined" && navigator.platform) 
        || (typeof window !== "undefined" && typeof window.navigator !== "undefined" && window.navigator.platform) 
        || "unknown"
    """
)
