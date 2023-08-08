/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.UnsafeNumber
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey
import platform.Foundation.NSUnderlyingErrorKey

@OptIn(UnsafeNumber::class)
internal fun Exception.toNSError() = NSError(
    domain = "Kotlin",
    code = 0,
    userInfo = mapOf(
        NSLocalizedDescriptionKey to message,
        NSUnderlyingErrorKey to this
    )
)
