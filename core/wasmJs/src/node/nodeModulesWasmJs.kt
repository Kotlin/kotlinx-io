/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

@JsFun("""
    (globalThis.module = (typeof process !== 'undefined') && (process.release.name === 'node') ?
        await import(/* webpackIgnore: true */'node:module') : void 0, () => {})
""")
internal external fun persistModule()

@JsFun("""() => { 
    const importMeta = import.meta;
    return globalThis.module.default.createRequire(importMeta.url);
}
""")
internal external fun getRequire(): JsAny

private val require = persistModule().let { getRequire() }

@JsFun("""
    (require, mod) => {
         try {
             let m = require(mod);
             if (m) return m;
             return null;
         } catch (e) {
             return null;
         }
    }
""")
internal external fun requireModule(require: JsAny, mod: String): JsAny?

internal fun loadModule(name: String): JsAny {
    val mod = requireModule(require, name) ?: throw UnsupportedOperationException("Module '$name' could not be imported")
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
