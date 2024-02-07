/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlin.test.Test
import kotlin.test.assertEquals

class PreOpensTest {
    @Test
    fun hasTemp() {
        val preopen = PreOpens.preopens.single()
        assertEquals(3, preopen.fd)
        assertEquals(Path("/tmp"), preopen.path)
    }
}
