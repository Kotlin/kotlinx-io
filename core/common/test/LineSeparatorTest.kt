/*
 * Copyright 2010-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.isWindows
import kotlin.test.Test
import kotlin.test.assertEquals

class LineSeparatorTest {
    @Test
    public fun testLineSeparator() {
        if (isWindows) {
            assertEquals("\r\n", SystemLineSeparator)
        } else {
            assertEquals("\n", SystemLineSeparator)
        }
    }
}
