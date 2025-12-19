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
    return if (windowNavigatorPlatform().startsWith("Win", ignoreCase = true)) "\r\n" else "\n"
}

@JsFun("() => window.navigator.platform")
private external fun windowNavigatorPlatform(): String
