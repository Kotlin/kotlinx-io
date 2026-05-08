/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlinx.io.node

internal actual val buffer: BufferModule by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (loadBuffer() ?: throwModuleCannotBeImported("path")) as BufferModule
}

@JsFun("${LOAD_MODULE_PREFIX}buffer${LOAD_MODULE_POSTFIX}")
private external fun loadBuffer(): JsAny?

internal actual val os: Os by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (loadOs() ?: throwModuleCannotBeImported("os")) as Os
}

@JsFun("${LOAD_MODULE_PREFIX}os${LOAD_MODULE_POSTFIX}")
private external fun loadOs(): JsAny?

internal actual val path: Path by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (loadPath() ?: throwModuleCannotBeImported("path")) as Path
}

@JsFun("${LOAD_MODULE_PREFIX}path${LOAD_MODULE_POSTFIX}")
private external fun loadPath(): JsAny?

internal actual val fs: Fs by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (loadFs() ?: throwModuleCannotBeImported("fs")) as Fs
}

@JsFun("${LOAD_MODULE_PREFIX}fs${LOAD_MODULE_POSTFIX}")
private external fun loadFs(): JsAny?

private fun throwModuleCannotBeImported(name: String) {
    throw UnsupportedOperationException("Module $name cannot be imported in this environment")
}

/*
Wasm JsFun expect something invokeable, so we have to return an arrow function.
IIFE which returns a function that returns an input parameter (resolved module).
It helps us to work around the issue that we cannot use await import in non-async arrow functions.
(
    (module) => {
        return () => module
    }
)(await import("module"))
 */
private const val LOAD_MODULE_PREFIX =
    "((module) => () => module)(((typeof process !== 'undefined') && (process.release.name === 'node')) ? await import(/* webpackIgnore: true */'node:"

private const val LOAD_MODULE_POSTFIX = "') : null)"
