/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

internal external interface Os {
    /**
     * See https://nodejs.org/api/os.html#ostmpdir
     */
    fun tmpdir(): String?


    /**
     * See https://nodejs.org/api/os.html#osplatform
     */
    fun platform(): String
}

internal val os: Os by lazy {
    loadModule("os", ::osInitializer)
}

private fun osInitializer(): Os? = js("eval('require')('os')")
