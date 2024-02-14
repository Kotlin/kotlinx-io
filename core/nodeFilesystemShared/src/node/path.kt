/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:CommonJsModule("path")
@file:CommonJsNonModule

package kotlinx.io.node.path

import kotlinx.io.CommonJsModule
import kotlinx.io.CommonJsNonModule

internal external fun isAbsolute(path: String): Boolean
internal external fun dirname(path: String): String
internal external fun basename(path: String): String

internal external val sep: String
