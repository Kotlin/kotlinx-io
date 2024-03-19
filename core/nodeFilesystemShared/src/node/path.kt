/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

internal external interface Path {

    fun isAbsolute(path: String): Boolean
    fun dirname(path: String): String
    fun basename(path: String): String

    val sep: String
}

internal val path: Path by lazy {
    loadModule("path", ::pathInitializer)
}

private fun pathInitializer(): Path? = js("eval('require')('path')")
