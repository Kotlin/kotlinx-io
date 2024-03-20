/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

internal actual fun bufferInitializer(): BufferModule? = js("eval('require')('buffer')")
internal actual fun osInitializer(): Os? = js("eval('require')('os')")
internal actual fun fsInitializer(): Fs? = js("eval('require')('fs')")
internal actual fun pathInitializer(): Path? = js("eval('require')('path')")
