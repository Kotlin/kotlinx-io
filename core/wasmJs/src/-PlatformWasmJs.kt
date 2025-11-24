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
 * In NodeJS-compatible environments, this property derives value from [os.EOL](https://nodejs.org/api/os.html#oseol),
 * in all other environments (like a web-browser), its value is always `"\n"`.
 */
public actual val SystemLineSeparator: String by lazy {
    try {
        os.EOL
    } catch (_: Throwable) {
        "\n"
    }
}
