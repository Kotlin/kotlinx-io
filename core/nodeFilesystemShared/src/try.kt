/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

/**
 * Kotlin/Wasm can't handle exceptions thrown by a JS runtime.
 * This function wraps a block that may potentially throw something and returns an exception if it was caught.
 */
internal expect fun withCaughtException(block: () -> Unit): Throwable?
