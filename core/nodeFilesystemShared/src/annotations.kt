/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

// The source set is shared by js and wasmJs targets that don't have a single common JS-source set.
// As a result, JsModule and JsNonModule annotations are unavailable. To overcome that issue,
// the following expects are declared in this module and actualized in the leaf source sets.

/**
 * Actualized with JsModule on both JS and WasmJs.
 */
@Target(AnnotationTarget.FILE)
internal expect annotation class CommonJsModule(val import: String)

/**
 * Actualized with JsNonModule on JS, left "empty" on WasmJs.
 */
@Target(AnnotationTarget.FILE)
internal expect annotation class CommonJsNonModule()
