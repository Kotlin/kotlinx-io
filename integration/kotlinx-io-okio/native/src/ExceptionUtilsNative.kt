/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.okio

import kotlinx.io.EOFException

internal actual fun Throwable.setCauseIfSupported(cause: Throwable?): Unit = Unit

internal actual fun newEOFExceptionWithCause(message: String?, cause: Throwable?): EOFException =
    EOFException(message, cause)
