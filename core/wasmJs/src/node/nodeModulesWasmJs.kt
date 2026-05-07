/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package kotlinx.io.node

@JsModule("node:buffer")
internal actual external val buffer: BufferModule

@JsModule("node:os")
internal actual external val os: Os

@JsModule("node:path")
internal actual external val path: Path

@JsModule("node:fs")
internal actual external val fs: Fs
