/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

@Suppress("ACTUAL_WITHOUT_EXPECT")
internal actual typealias CommonJsModule = JsModule

@Target(AnnotationTarget.FILE)
internal actual annotation class CommonJsNonModule()

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

