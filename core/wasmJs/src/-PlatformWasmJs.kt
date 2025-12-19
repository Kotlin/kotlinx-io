/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.node.os

internal class JsException(message: String) : RuntimeException(message)

internal actual fun withCaughtException(block: () -> Unit): Throwable? {
    val e = catchJsThrowable(block) ?: return null
    return JsException(e.toString())
}

private fun catchJsThrowable(block: () -> Unit): JsAny? = js("""{
    try {
        block();
        return null;
    } catch (e) {
        if (e.message) return e.message;
        if (e && e.toString) return e.toString();
        return e + "";
    }
}""")

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

@JsFun("""
    () =>
        (typeof navigator !== "undefined" && navigator.platform)
        || (typeof window !== "undefined" && typeof window.navigator !== "undefined" && window.navigator.platform)
        || "unknown"
""")
private external fun windowNavigatorPlatform(): String

internal actual val isWindows by lazy {
    windowNavigatorPlatform().startsWith("Win", ignoreCase = true)
}
