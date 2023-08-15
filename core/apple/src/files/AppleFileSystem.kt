/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.Foundation.NSTemporaryDirectory
import platform.posix.dirname
import platform.posix.errno
import platform.posix.rename
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
internal actual fun atomicMoveImpl(source: Path, destination: Path) {
    if (rename(source.path, destination.path) != 0) {
        throw IOException("Move failed: ${strerror(errno)?.toKString()}")
    }
}

internal actual val NativeTempDir: Path
    get() = Path(NSTemporaryDirectory())

@OptIn(ExperimentalForeignApi::class)
internal actual fun dirnameImpl(path: String): String {
    val p = dirname(path.cstr)?.toKString() ?: ""
    return p
}
