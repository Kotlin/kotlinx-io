/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.*
import platform.posix.dirname

@OptIn(ExperimentalForeignApi::class)
internal actual fun dirnameImpl(path: String): String {
    memScoped {
        val cstr = path.cstr
        println(buildString {
            for (i in path.indices) {
                val b = cstr.ptr[i]
                if (b.toInt() == 0) break
                append(b)
                append(' ')
            }
        })
        val res = dirname(cstr) ?: return ""
        println(buildString {
            for (i in path.indices) {
                val b = res[i]
                if (b.toInt() == 0) break
                append(b)
                append(' ')
            }
        })
        return res.toKString()
    }
}

