/*
* Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
* Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
*/
package kotlinx.io.bytestring

import kotlin.concurrent.*

@Suppress("ACTUAL_WITHOUT_EXPECT") // This suppress can be removed in 2.0: KT-59355
internal actual typealias BenignDataRace = Volatile
