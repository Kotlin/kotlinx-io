/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:CommonJsModule("os")
@file:CommonJsNonModule

package kotlinx.io.node.os

import kotlinx.io.CommonJsModule
import kotlinx.io.CommonJsNonModule

/**
 * See https://nodejs.org/api/os.html#ostmpdir
 */
internal external fun tmpdir(): String?


/**
 * See https://nodejs.org/api/os.html#osplatform
 */
internal external fun platform(): String
