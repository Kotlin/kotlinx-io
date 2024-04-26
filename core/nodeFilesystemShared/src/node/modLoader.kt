/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

import kotlinx.io.withCaughtException

internal fun <T> loadModule(name: String, initializer: () -> T?): T {
    var mod: T? = null
    val ex = withCaughtException {
        mod = initializer()
    }
    if (mod == null) {
        throw UnsupportedOperationException("Module '$name' could not be loaded", ex)
    }
    return mod!!
}
