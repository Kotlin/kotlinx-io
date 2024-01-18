/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

internal val os: dynamic
    get(): dynamic {
        return try {
            js("require('os')")
        } catch (t: Throwable) {
            null
        }
    }

internal val fs: dynamic
    get(): dynamic {
        return try {
            js("require('fs')")
        } catch (t: Throwable) {
            null
        }
    }

internal val buffer: dynamic
    get(): dynamic {
        return try {
            js("require('buffer')")
        } catch (t: Throwable) {
            null
        }
    }

internal val pathLib: dynamic
    get(): dynamic {
        return try {
            js("require('path')")
        } catch (t: Throwable) {
            null
        }
    }
