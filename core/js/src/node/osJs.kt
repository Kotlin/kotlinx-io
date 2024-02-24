/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.os

import kotlinx.io.node.loadModule

private val osModule: dynamic by lazy {
    loadModule("os") { js("require('os')") }
}

/**
 * See https://nodejs.org/api/os.html#ostmpdir
 */
internal actual fun tmpdir(): String? = osModule.tmpdir() as String?

/**
 * See https://nodejs.org/api/os.html#osplatform
 */
internal actual fun platform(): String = osModule.platform() as String
