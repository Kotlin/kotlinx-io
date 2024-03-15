/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

internal actual val path: Path by lazy {
    try {
        js("require(\"path\")")
    } catch (e: Throwable) {
        throw UnsupportedOperationException("Module 'path' could not be imported", e)
    }
}

internal actual val fs: Fs by lazy {
    try {
        js("require(\"fs\")")
    } catch (e: Throwable) {
        throw UnsupportedOperationException("Module 'fs' could not be imported", e)
    }
}

internal actual val os: Os by lazy {
    try {
        js("require(\"os\")")
    } catch (e: Throwable) {
        throw UnsupportedOperationException("Module 'os' could not be imported", e)
    }
}

internal actual val buffer: BufferModule by lazy {
    try {
        js("require(\"buffer\")")
    } catch (e: Throwable) {
        throw UnsupportedOperationException("Module 'buffer' could not be imported", e)
    }
}

