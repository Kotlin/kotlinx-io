/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.path

internal expect fun isAbsolute(path: String): Boolean
internal expect fun dirname(path: String): String
internal expect fun basename(path: String): String

internal expect val sep: String
