/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.*


public actual class Path internal constructor(private val path: String,
                                              @Suppress("UNUSED_PARAMETER") any: Any?) {
    override fun toString(): String = path
}

public actual fun Path(path: String): Path {
    return Path(path, null)
}

public actual fun Path.source(): Source {
    TODO()
}

public actual fun Path.sink(): Sink {
    TODO()
}
