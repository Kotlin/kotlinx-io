/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.os

import kotlinx.io.node.loadModule

private val osModule: JsAny by lazy {
    loadModule("os")
}

/**
 * See https://nodejs.org/api/os.html#ostmpdir
 */
internal actual fun tmpdir(): String? = tmpdir(osModule)

private fun tmpdir(mod: JsAny): String? = js("mod.tmpdir()")

/**
 * See https://nodejs.org/api/os.html#osplatform
 */
internal actual fun platform(): String = platform(osModule)

private fun platform(mod: JsAny): String = js("mod.platform()")
