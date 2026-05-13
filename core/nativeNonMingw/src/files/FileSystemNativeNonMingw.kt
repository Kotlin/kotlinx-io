/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import platform.posix.O_CLOEXEC

internal actual val DefaultOpenFlags: Int
    get() = O_CLOEXEC
