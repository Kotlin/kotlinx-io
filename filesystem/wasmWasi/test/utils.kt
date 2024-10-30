/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
actual fun tempFileName(): String =
    Path(SystemTemporaryDirectory, Random.nextBytes(32).toHexString()).toString()
