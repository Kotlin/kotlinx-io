/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SmokeFileTestWindowJS  {
    @Test
    fun uncParent() {
        if (!isWindows) return
        // NodeJS, correctly, assume that it's a root path, that's where behavior diverge
        assertNull(Path("\\\\server\\share").parent)
        assertEquals(Path("\\\\server\\share"), Path("\\\\server\\share\\dir").parent)
    }
}
