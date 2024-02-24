/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.path

import kotlinx.io.node.loadModule

private val pathModule: JsAny by lazy {
    loadModule("path")
}

private fun isAbsolute(path: String, mod: JsAny): Boolean = js("mod.isAbsolute(path)")

internal actual fun isAbsolute(path: String): Boolean = isAbsolute(path, pathModule)

private fun dirname(path: String, mod: JsAny): String = js("mod.dirname(path)")

internal actual fun dirname(path: String): String = dirname(path, pathModule)

private fun basename(path: String, mod: JsAny): String = js("mod.basename(path)")

internal actual fun basename(path: String): String = basename(path, pathModule)

private fun sep(mod: JsAny): String = js("mod.sep")

internal actual val sep: String
    get() = sep(pathModule)

