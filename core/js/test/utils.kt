/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlin.random.Random

private val os: dynamic
    get(): dynamic {
        return try {
            js("require('os')")
        } catch (t: Throwable) {
            null
        }
    }

private val fs: dynamic
    get(): dynamic {
        return try {
            js("require('fs')")
        } catch (t: Throwable) {
            null
        }
    }

private val nodePath: dynamic
    get(): dynamic {
        return try {
            js("require('path')")
        } catch (t: Throwable) {
            null
        }
    }

@OptIn(ExperimentalStdlibApi::class)
actual fun tempFileName(): String {
    while (true) {
        val tmpdir = os.tmpdir()
        val filename = Random.nextBytes(32).toHexString()
        val fullpath = "$tmpdir${nodePath.sep}$filename"

        if (fs.existsSync(fullpath) as Boolean) {
            continue
        }
        return fullpath
    }
}

actual val isWindows: Boolean = isWindows_
