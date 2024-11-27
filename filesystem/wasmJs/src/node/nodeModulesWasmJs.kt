/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

internal fun requireExists(): Boolean = js("typeof require === 'function'")

internal fun requireModule(mod: String): JsAny? = js("""{
     try {
         let m = require(mod);
         if (m) return m;
         return null;
     } catch (e) {
         return null;
     }
 }""")

internal fun loadModule(name: String): JsAny {
    if (!requireExists()) {
        throw UnsupportedOperationException("Module $name could not be loaded")
    }
    val mod = requireModule(name) ?: throw UnsupportedOperationException("Module '$name' could not be imported")
    return mod
}

internal actual val buffer: BufferModule by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    loadModule("buffer") as BufferModule
}

internal actual val os: Os  by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    loadModule("os") as Os
}

internal actual val path: Path by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    loadModule("path") as Path
}

internal actual val fs: Fs by lazy {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    loadModule("fs") as Fs
}
