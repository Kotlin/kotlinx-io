/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.path

import kotlinx.io.node.loadModule

private val pathModule: dynamic by lazy {
    loadModule("path") { js("require('path')") }
}

internal actual fun isAbsolute(path: String): Boolean = pathModule.isAbsolute(path) as Boolean

internal actual fun dirname(path: String): String = pathModule.dirname(path) as String

internal actual fun basename(path: String): String = pathModule.basename(path) as String

internal actual val sep: String
    get() = pathModule.sep as String
