/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

internal inline fun loadModule(name: String, loadBlock: () -> dynamic): dynamic {
    val mod = try {
        loadBlock()
    } catch (e: Throwable) {
        null
    }
    if (mod == null) {
        throw UnsupportedOperationException("Module $name could not be loaded")
    }
    return mod
}
